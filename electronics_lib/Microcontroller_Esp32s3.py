from abc import abstractmethod
from typing import *

from electronics_abstract_parts import *
from .JlcPart import JlcPart
from .Microcontroller_Esp import HasEspProgramming


@non_library
class Esp32s3_Ios(IoControllerI2s, BaseIoControllerPinmapGenerator):
  """IOs definitions independent of infrastructural (e.g. power) pins."""
  @abstractmethod
  def _generator_gnd_vddio(self) -> Tuple[Port[VoltageLink], Port[VoltageLink]]:
    """Returns GND and VDDIO (either source or sink). Only called within a generator. No side effects."""
    ...

  def _generator_dio_model(self) -> CircuitPort[DigitalLink]:
    """Returns a digital IO model. Only called within a generator."""
    ...

  def _io_pinmap(self) -> PinMapUtil:
    gnd, pwr = self._generator_gnd_vddio()
    dio_model = self._generator_dio_model()

    adc_model = AnalogSink.from_supply(gnd, pwr)  # table 4-5, no other specs given

    uart_model = UartPort(DigitalBidir.empty())  # section 3.5.5, up to 5Mbps
    spi_model = SpiMaster(DigitalBidir.empty(), (0, 80)*MHertz)  # section 3.5.2, 80MHz in master, 60MHz in slave
    i2c_model = I2cMaster(DigitalBidir.empty())  # section 3.5.6, 100/400kHz and up to 800kbit/s
    can_model = CanControllerPort(DigitalBidir.empty())  # aka TWAI, up to 1Mbit/s
    i2s_model = I2sController(DigitalBidir.empty())

    return PinMapUtil([  # table 2-1 for overview, table 3-3 for remapping, table 2-4 for ADC
      # VDD3P3_RTC domain
      # PinResource('GPIO0', {'GPIO0': self._dio_model}),  # strapping pin, boot mode
      PinResource('GPIO1', {'GPIO1': dio_model, 'ADC1_CH0': adc_model}),
      PinResource('GPIO2', {'GPIO2': dio_model, 'ADC1_CH1': adc_model}),
      # technically a strapping pin for JTAG control, but needs to be enabled by eFuse
      PinResource('GPIO3', {'GPIO3': dio_model, 'ADC1_CH2': adc_model}),
      PinResource('GPIO4', {'GPIO4': dio_model, 'ADC1_CH3': adc_model}),
      PinResource('GPIO5', {'GPIO5': dio_model, 'ADC1_CH4': adc_model}),
      PinResource('GPIO6', {'GPIO6': dio_model, 'ADC1_CH5': adc_model}),
      PinResource('GPIO7', {'GPIO7': dio_model, 'ADC1_CH6': adc_model}),
      PinResource('GPIO8', {'GPIO8': dio_model, 'ADC1_CH7': adc_model}),
      PinResource('GPIO9', {'GPIO9': dio_model, 'ADC1_CH8': adc_model}),
      PinResource('GPIO10', {'GPIO10': dio_model, 'ADC1_CH9': adc_model}),
      # ADC2 pins can't be used simultaneously with WiFi (section 2.3.3) and are not allocatable
      PinResource('GPIO11', {'GPIO11': dio_model}),  # also ADC2_CH0
      PinResource('GPIO12', {'GPIO12': dio_model}),  # also ADC2_CH1
      PinResource('GPIO13', {'GPIO13': dio_model}),  # also ADC2_CH2
      PinResource('GPIO14', {'GPIO14': dio_model}),  # also ADC2_CH3

      PinResource('XTAL_32K_P', {'GPIO15': dio_model}),  # also ADC2_CH4
      PinResource('XTAL_32K_N', {'GPIO16': dio_model}),  # also ADC2_CH5

      PinResource('GPIO17', {'GPIO17': dio_model}),  # also ADC2_CH6
      PinResource('GPIO18', {'GPIO18': dio_model}),  # also ADC2_CH7
      PinResource('GPIO19', {'GPIO19': dio_model}),  # also ADC2_CH8 / USB_D-
      PinResource('GPIO20', {'GPIO20': dio_model}),  # also ADC2_CH9 / USB_D+
      PinResource('GPIO21', {'GPIO21': dio_model}),

      # VDD_SPI domain
      # section 2.3.3, these are allocated for flash and should not be used
      # PinResource('SPICS1', {'GPIO26': dio_model}),
      # PinResource('SPIHD', {'GPIO27': dio_model}),
      # PinResource('SPIWP', {'GPIO28': dio_model}),
      # PinResource('SPICS0', {'GPIO29': dio_model}),
      # PinResource('SPICLK', {'GPIO30': dio_model}),
      # PinResource('SPIQ', {'GPIO31': dio_model}),
      # PinResource('SPID', {'GPIO32': dio_model}),

      # VDD_SPI / VDD3P3_CPU domain
      PinResource('SPICLK_N', {'GPIO48': dio_model}),  # appendix A
      PinResource('SPICLK_P', {'GPIO47': dio_model}),  # appendix A
      # these may be allocated for PSRAM and should not be used
      # PinResource('GPIO33', {'GPIO33': dio_model}),
      # PinResource('GPIO34', {'GPIO34': dio_model}),
      # PinResource('GPIO35', {'GPIO35': dio_model}),
      # PinResource('GPIO36', {'GPIO36': dio_model}),
      # PinResource('GPIO37', {'GPIO37': dio_model}),

      # VDD3P3_CPU domain
      PinResource('GPIO38', {'GPIO38': dio_model}),
      PinResource('MTCK', {'GPIO39': dio_model}),
      PinResource('MTDO', {'GPIO40': dio_model}),
      PinResource('MTDI', {'GPIO41': dio_model}),
      PinResource('MTMS', {'GPIO42': dio_model}),

      # PinResource('U0TXD', {'GPIO43': dio_model}),  # for programming
      # PinResource('U0RXD', {'GPIO44': dio_model}),  # for programming
      # PeripheralFixedResource('U0', uart_model, {
      #   'tx': ['GPIO43'], 'rx': ['GPIO44']
      # }),

      # PinResource('GPIO45', {'GPIO45': dio_model}),  # strapping pin, VDD_SPI power source
      # PinResource('GPIO46', {'GPIO46': dio_model}),  # strapping pin, boot mode, keep low

      PeripheralAnyResource('U1', uart_model),
      PeripheralAnyResource('U2', uart_model),
      PeripheralAnyResource('I2CEXT0', i2c_model),
      PeripheralAnyResource('I2CEXT1', i2c_model),
      # SPI0/1 may be used for (possibly on-chip) flash / PSRAM
      PeripheralAnyResource('SPI2', spi_model),
      PeripheralAnyResource('SPI3', spi_model),
      PeripheralAnyResource('TWAI', can_model),
      PeripheralAnyResource('I2S0', i2s_model),
      PeripheralAnyResource('I2S1', i2s_model),

      PeripheralFixedResource('USB', UsbDevicePort.empty(), {
        'dp': ['GPIO20'], 'dm': ['GPIO19']
      }),
    ]).remap_pins(self.RESOURCE_PIN_REMAP)


@abstract_block
class Esp32s3_Device(Esp32s3_Ios, InternalSubcircuit, GeneratorBlock, FootprintBlock):
  """Base class for ESP32-S3 series microcontrollers with WiFi and Bluetooth (classic and LE)
  and AI acceleration

  Chip datasheet: https://www.espressif.com/documentation/esp32-s3_datasheet_en.pdf
  """
  SYSTEM_PIN_REMAP: Dict[str, Union[str, List[str]]]  # pin name in base -> pin name(s)
  RESOURCE_PIN_REMAP: Dict[str, str]  # resource name in base -> pin name

  def _system_pinmap(self) -> Dict[str, CircuitPort]:
    return VariantPinRemapper({
      'VDD': self.pwr,  # including VDD3V3, VDD3P3_RTC, VDD_SPI, VDD3P3_CPU
      'CHIP_PU': self.chip_pu,
      'GND': self.gnd,

      'GPIO0': self.io0,
      'U0RXD': self.uart0.rx,
      'U0TXD': self.uart0.tx,
    }).remap(self.SYSTEM_PIN_REMAP)

  def __init__(self, **kwargs) -> None:
    super().__init__(**kwargs)

    self.pwr = self.Port(VoltageSink(  # assumes single-rail module
      voltage_limits=(3.0, 3.6)*Volt,  # table 4-2
      current_draw=(0.001, 355)*mAmp + self.io_current_draw.upper()  # from power off (table 4-8) to RF working (table 12 on WROOM datasheet)
    ), [Power])
    self.gnd = self.Port(Ground(), [Common])

    self._dio_model = dio_model = DigitalBidir.from_supply(  # table 4-4
      self.gnd, self.pwr,
      voltage_limit_tolerance=(-0.3, 0.3) * Volt,
      current_limits=(-28, 40)*mAmp,
      input_threshold_factor=(0.25, 0.75),
      pullup_capable=True, pulldown_capable=True,
    )

    self.chip_pu = self.Port(dio_model, optional=True)  # table 2-5, power up/down control, do NOT leave floating
    self.has_chip_pu = self.Parameter(BoolExpr())  # but some modules connect it internally
    self.require(self.has_chip_pu == self.chip_pu.is_connected(), "EN not connected")
    self.io0 = self.Port(dio_model, optional=True)  # table 2-11, default pullup (SPI boot), set low to download boot
    self.uart0 = self.Port(UartPort(dio_model), optional=True)  # programming


class Esp32s3_Wroom_1_Device(Esp32s3_Device, FootprintBlock, JlcPart):
  SYSTEM_PIN_REMAP: Dict[str, Union[str, List[str]]] = {
    'VDD': '2',
    'GND': ['1', '40', '41'],  # 41 is EP
    'CHIP_PU': '3',  # aka EN

    'GPIO0': '27',

    'U0RXD': '36',
    'U0TXD': '37',
  }

  RESOURCE_PIN_REMAP = {
    'GPIO4': '4',
    'GPIO5': '5',
    'GPIO6': '6',
    'GPIO7': '7',
    'XTAL_32K_P': '8',  # GPIO15
    'XTAL_32K_N': '9',  # GPIO16
    'GPIO17': '10',
    'GPIO18': '11',
    'GPIO8': '12',
    'GPIO19': '13',
    'GPIO20': '14',
    'GPIO3': '15',
    # 'GPIO46': '16',  # strapping pin
    'GPIO9': '17',
    'GPIO10': '18',
    'GPIO11': '19',
    'GPIO12': '20',
    'GPIO13': '21',
    'GPIO14': '22',
    'GPIO21': '23',
    'SPICLK_P': '24',  # GPIO47
    'SPICLK_N': '25',  # GPIO48
    # 'GPIO45': '26',  # strapping pin
    # 'GPIO35': '28',  # not available on PSRAM variants
    # 'GPIO36': '29',  # not available on PSRAM variants
    # 'GPIO37': '30',  # not available on PSRAM variants
    'GPIO38': '31',
    'MTCK': '32',  # GPIO39
    'MTDO': '33',  # GPIO40
    'MTDI': '34',  # GPIO41
    'MTMS': '35',  # GPIO42
    'GPIO2': '38',
    'GPIO1': '39',
  }

  def generate(self) -> None:
    super().generate()

    self.assign(self.has_chip_pu, True)

    self.assign(self.lcsc_part, 'C2913202')  # note standard only assembly
    self.assign(self.actual_basic_part, False)
    self.footprint(
      'U', 'RF_Module:ESP32-S3-WROOM-1',
      self._make_pinning(),
      mfr='Espressif Systems', part='ESP32-S3-WROOM-1-N16R8',  # higher end variant
      datasheet='https://www.espressif.com/documentation/esp32-s3-wroom-1_wroom-1u_datasheet_en.pdf',
    )


class Esp32s3_Wroom_1(Microcontroller, Radiofrequency, IoControllerI2s, HasEspProgramming,
                      IoController, BaseIoControllerExportable):
  """ESP32-S3-WROOM-1 module
  """
  def contents(self) -> None:
    super().contents()

    with self.implicit_connect(
        ImplicitConnect(self.pwr, [Power]),
        ImplicitConnect(self.gnd, [Common])
    ) as imp:
      self.ic = imp.Block(Esp32s3_Wroom_1_Device(pin_assigns=ArrayStringExpr()))
      self.connect(self.program_uart_node, self.ic.uart0)
      self.connect(self.program_en_node, self.ic.chip_pu)
      self.connect(self.program_boot_node, self.ic.io0)

      self.vcc_cap0 = imp.Block(DecouplingCapacitor(22 * uFarad(tol=0.2)))  # C1
      self.vcc_cap1 = imp.Block(DecouplingCapacitor(0.1 * uFarad(tol=0.2)))  # C2

      self.en_pull = imp.Block(PullupDelayRc(10 * kOhm(tol=0.05), 10*mSecond(tol=0.2))).connected(io=self.ic.chip_pu)


class Freenove_Esp32s3_Wroom_Device(Esp32s3_Device, FootprintBlock):
  """Freenove ESP32S3 WROOM breakout breakout with camera.

  Board pinning: https://github.com/Freenove/Freenove_ESP32_S3_WROOM_Board/blob/main/ESP32S3_Pinout.png

  Top left is pin 1, going down the left side then up the right side.
  Up is defined from the text orientation (antenna is on top).
  """
  SYSTEM_PIN_REMAP: Dict[str, Union[str, List[str]]] = {
    'VDD': '1',  # 3v3, output of internal AMS1117-3.3V LDO
    # 20: Vcc 5vUSB
    'GND': '21',
    'CHIP_PU': '2',  # aka EN, switch w/ pullup on board

    'GPIO2': '39',  # fixed strapping pin, drives LED on PCB
    'GPIO0': '28',  # fixed strapping pin

    'U0RXD': '39',  # fixed programming pin, board connected to USB UART w/ jumper
    'U0TXD': '40',  # fixed programming pin, board connected to USB UART w/ jumper
  }
  RESOURCE_PIN_REMAP = {
    # 'GPIO4': '3',  # CAM_SIOD
    # 'GPIO5': '4',  # CAM_SIOC
    # 'GPIO6': '5',  # CAM_VYSNC
    # 'GPIO7': '6',  # CAM_HREF
    # 'GPIO15': '7',  # CAM_XCLK
    # 'GPIO16': '8',  # CAM_Y9
    # 'GPIO17': '9',  # CAM_Y8
    # 'GPIO18': '10',  # CAM_Y7
    # 'GPIO8': '11',  # CAM_Y4
    'GPIO3': '12',
    # 'GPIO46': '13',  # strapping pin, boot mode
    # 'GPIO9': '14',  # CAM_Y3
    # 'GPIO10': '15',  # CAM_Y5
    # 'GPIO11': '16',  # CAM_Y2
    # 'GPIO12': '17',  # CAM_Y6
    # 'GPIO13': '18',  # CAM_PCLK
    'GPIO14': '19',

    # 'GPIO19': '22',  # USB_D+
    # 'GPIO20': '23',  # USB_D-
    'GPIO21': '24',
    'SPICLK_P': '25',  # GPIO47
    'SPICLK_N': '26',  # GPIO48, internal WS2812
    # 'GPIO45': '27',  # strapping pin, VDD_SPI
    # 'GPIO35': '29',  # PSRAM
    # 'GPIO36': '30',  # PSRAM
    # 'GPIO37': '31',  # PSRAM
    # 'GPIO38': '32',  # SD_CMD
    # 'GPIO39': '33',  # SD_CLK
    # 'GPIO40': '34',  # SD_DATA
    'MTDI': '35',  # GPIO41
    'MTMS': '36',  # GPIO42
    'GPIO2': '37',  # internal LED
    'GPIO1': '38',
  }

  def _io_pinmap(self) -> PinMapUtil:  # allow the camera I2C pins to be used externally
    return super()._io_pinmap().add([
      PeripheralFixedPin('CAM_SCCB', I2cMaster(self._dio_model, has_pullup=True), {
        'scl': '4', 'sda': '3'
      })
    ])

  def __init__(self, **kawrgs) -> None:
    super().__init__(**kawrgs)

    self.vusb = self.Port(VoltageSource(
      voltage_out=UsbConnector.USB2_VOLTAGE_RANGE,
      current_limits=UsbConnector.USB2_CURRENT_LIMITS
    ), optional=True)

  def generate(self) -> None:
    super().generate()
    self.assign(self.has_chip_pu, False)

    pinning = self._make_pinning()  # add optional output pins
    pinning['20'] = self.vusb

    self.footprint(
      'U', 'edg:Freenove_ESP32-WROVER',
      pinning,
      mfr='', part='Freenove ESP32S3-WROOM',
    )


class Freenove_Esp32s3_Wroom(Microcontroller, Radiofrequency, IoControllerUsbOut, IoControllerPowerOut, IoControllerI2s,
                             IoController, BaseIoControllerExportable, GeneratorBlock):
  """Wrapper around Esp32_Wover_Dev fitting the IoController interface
  """
  POWER_REQUIRED = False

  def __init__(self):
    super().__init__()

    self.ic: Freenove_Esp32s3_Wroom_Device = self.Block(Freenove_Esp32s3_Wroom_Device(pin_assigns=ArrayStringExpr()))

    self.generator_param(self.gnd.is_connected())
    self.generator_param(self.gnd_out.is_connected())

    self.generator_param(self.pwr.is_connected())
    self.generator_param(self.pwr_out.is_connected())
    self.generator_param(self.vusb_out.is_connected())

  def generate(self) -> None:
    super().generate()

    if self.get(self.gnd.is_connected()):  # board sinks power
      self.connect(self.gnd, self.ic.gnd)
      self.connect(self.pwr, self.ic.pwr)

      self.require(~self.vusb_out.is_connected(), "can't source USB power if source gnd not connected")
      self.require(~self.pwr_out.is_connected(), "can't source 3v3 power if source gnd not connected")
      self.require(~self.gnd_out.is_connected(), "can't source gnd if source gnd not connected")
    else:  # board sources power (default)
      self.gnd_source = self.Block(DummyVoltageSource(
        voltage_out=0*Volt(tol=0),
        current_limits=Range.all()
      ))
      self.connect(self.gnd_source.pwr, self.ic.gnd, self.gnd_out)

      self.pwr_source = self.Block(DummyVoltageSource(
        voltage_out=3.3*Volt(tol=0.05),  # tolerance is a guess
        current_limits=UsbConnector.USB2_CURRENT_LIMITS
      ))
      self.connect(self.pwr_source.pwr, self.ic.pwr, self.pwr_out)

      self.connect(self.vusb_out, self.ic.vusb)

      self.require(~self.pwr.is_connected(), "can't sink power if source gnd connected")
      self.require(~self.gnd.is_connected(), "can't sink gnd if source gnd connected")
