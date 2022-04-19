import unittest

from edg import *


class TestBlinkyBasic(BoardTop):
  def contents(self):
    super().contents()
    self.mcu = self.Block(Nucleo_F303k8())
    self.dummy = self.Block(VoltageLoad())
    self.connect(self.mcu.pwr_3v3, self.dummy.pwr)  # TODO this is a hack to define the 3v3 link

    self.led = self.Block(IndicatorLed())
    self.connect(self.mcu.gnd, self.led.gnd)
    self.connect(self.mcu.gpio.allocate(), self.led.signal)

  def refinements(self) -> Refinements:
    return super().refinements() + Refinements(
    )


class TestBlinkySimple(BoardTop):
  def contents(self):
    super().contents()
    self.mcu = self.Block(Nucleo_F303k8())
    self.dummy = self.Block(VoltageLoad())
    self.connect(self.mcu.pwr_3v3, self.dummy.pwr)  # TODO this is a hack to define the 3v3 link

    with self.implicit_connect(
        ImplicitConnect(self.mcu.gnd, [Common]),
    ) as imp:
      self.led = imp.Block(IndicatorLed())
      self.sw = imp.Block(DigitalSwitch())

    self.connect(self.mcu.gpio.allocate(), self.led.signal)
    self.connect(self.sw.out, self.mcu.gpio.allocate())

  def refinements(self) -> Refinements:
    return super().refinements() + Refinements(
    )


class TestBlinkySimpleChain(BoardTop):
  def contents(self):
    super().contents()
    self.mcu = self.Block(Nucleo_F303k8())
    self.dummy = self.Block(VoltageLoad())
    self.connect(self.mcu.pwr_3v3, self.dummy.pwr)  # TODO this is a hack to define the 3v3 link

    with self.implicit_connect(
        ImplicitConnect(self.mcu.gnd, [Common]),
    ) as imp:
      (self.led, ), _ = self.chain(self.mcu.gpio.allocate(), imp.Block(IndicatorLed()))
      (self.sw, ), _ = self.chain(imp.Block(DigitalSwitch()), self.mcu.gpio.allocate())

  def refinements(self) -> Refinements:
    return super().refinements() + Refinements(
    )


class TestBlinkyBroken(BoardTop):
  def contents(self):
    super().contents()
    self.usb = self.Block(UsbDeviceCReceptacle())
    self.vusb = self.connect(self.usb.pwr)
    self.gnd = self.connect(self.usb.gnd)

    with self.implicit_connect(
        ImplicitConnect(self.vusb, [Power]),
        ImplicitConnect(self.gnd, [Common]),
    ) as imp:
      self.mcu = imp.Block(IoController())

  def refinements(self) -> Refinements:
    return super().refinements() + Refinements(
      instance_refinements=[
        (['mcu'], Lpc1549_48),
      ]
    )


class TestBlinkyFlattened(BoardTop):
  def contents(self):
    super().contents()
    self.usb = self.Block(UsbDeviceCReceptacle())
    self.vusb = self.connect(self.usb.pwr)
    self.gnd = self.connect(self.usb.gnd)

    with self.implicit_connect(
        ImplicitConnect(self.vusb, [Power]),
        ImplicitConnect(self.gnd, [Common]),
    ) as imp:
      self.usb_reg = imp.Block(BuckConverter(output_voltage=3.3*Volt(tol=0.05)))

    self.v3v3 = self.connect(self.usb_reg.pwr_out)

    with self.implicit_connect(
        ImplicitConnect(self.v3v3, [Power]),
        ImplicitConnect(self.gnd, [Common]),
    ) as imp:
      self.mcu = imp.Block(IoController())

      (self.led, ), _ = self.chain(self.mcu.gpio.allocate(), imp.Block(IndicatorLed()))
      (self.sw, ), _ = self.chain(imp.Block(DigitalSwitch()), self.mcu.gpio.allocate())

  def refinements(self) -> Refinements:
    return super().refinements() + Refinements(
      instance_refinements=[
        (['mcu'], Lpc1549_48),
        (['usb_reg'], Tps561201),
      ],
    )


class Mcp9700_Device(FootprintBlock):
  def __init__(self) -> None:
    super().__init__()
    # block boundary (ports, parameters) definition here
    self.vdd = self.Port(VoltageSink(
      voltage_limits=(2.3, 5.5)*Volt, current_draw=(0, 15)*uAmp
    ), [Power])
    self.vout = self.Port(AnalogSource(
      voltage_out=(0.1, 2), current_limits=(0, 100)*uAmp,
      impedance=(20, 20)*Ohm
    ), [Output])
    self.gnd = self.Port(Ground(), [Common])

  def contents(self) -> None:
    super().contents()
    # block implementation (subblocks, internal connections, footprint) here
    self.footprint(
      'U', 'Package_TO_SOT_SMD:SOT-23',
      {
        '1': self.vdd,
        '2': self.vout,
        '3': self.gnd,
      },
      mfr='Microchip Technology', part='MCP9700T-E/TT',
      datasheet='http://ww1.microchip.com/downloads/en/DeviceDoc/20001942G.pdf'
    )


class Mcp9700(Block):
  def __init__(self) -> None:
    super().__init__()
    self.ic = self.Block(Mcp9700_Device())
    self.pwr = self.Export(self.ic.vdd, [Power])
    self.gnd = self.Export(self.ic.gnd, [Common])
    self.out = self.Export(self.ic.vout, [Output])

  def contents(self) -> None:
    super().contents()
    self.vdd_cap = self.Block(DecouplingCapacitor(
      capacitance=0.1*uFarad(tol=0.2)
    )).connected(self.gnd, self.pwr)


class TestBlinkyComplete(BoardTop):
  def contents(self):
    super().contents()
    self.usb = self.Block(UsbDeviceCReceptacle())

    self.vusb = self.connect(self.usb.pwr)
    self.gnd = self.connect(self.usb.gnd)

    with self.implicit_connect(
        ImplicitConnect(self.vusb, [Power]),
        ImplicitConnect(self.gnd, [Common]),
    ) as imp:
      self.usb_reg = imp.Block(BuckConverter(output_voltage=3.3*Volt(tol=0.05)))

    self.v3v3 = self.connect(self.usb_reg.pwr_out)

    with self.implicit_connect(
        ImplicitConnect(self.v3v3, [Power]),
        ImplicitConnect(self.gnd, [Common]),
    ) as imp:
      self.mcu = imp.Block(IoController())

      self.led = ElementDict[IndicatorLed]()
      for i in range(8):
        (self.led[i], ), _ = self.chain(self.mcu.gpio.allocate(), imp.Block(IndicatorLed()))

      (self.sw, ), _ = self.chain(imp.Block(DigitalSwitch()), self.mcu.gpio.allocate())
      (self.temp, ), _ = self.chain(imp.Block(Mcp9700()), self.mcu.adc.allocate())

  def refinements(self) -> Refinements:
    return super().refinements() + Refinements(
      instance_refinements=[
        (['mcu'], Lpc1549_48),
        (['usb_reg'], Tps561201),
      ],
    )


class BlinkyTestCase(unittest.TestCase):
  def test_design_basic(self) -> None:
    compile_board_inplace(TestBlinkyBasic)

  def test_design_simple(self) -> None:
    compile_board_inplace(TestBlinkySimple)

  def test_design_simple_chain(self) -> None:
    compile_board_inplace(TestBlinkySimpleChain)

  def test_design_broken(self) -> None:
    with self.assertRaises(CompilerCheckError):
      compile_board_inplace(TestBlinkyBroken)

  def test_design_flat(self) -> None:
    compile_board_inplace(TestBlinkyFlattened)

  def test_design_complete(self) -> None:
    compile_board_inplace(TestBlinkyComplete)
