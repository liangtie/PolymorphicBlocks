from electronics_abstract_parts import *
from . import PassiveConnector
from .JlcPart import JlcPart


# Because we have both the chip + application circuit and the connector versions of the device,
# the shared definitions are here
# TODO is there a better way to model this?
def make_vl53l0x_vdd_model() -> VoltageSink:
  return VoltageSink(
    voltage_limits=(2.6, 3.5) * Volt,
    current_draw=(3, 40000) * uAmp  # up to 40mA including VCSEL when ranging
  )

def make_vl53l0x_gpio_model(vss: Port[VoltageLink], vdd: Port[VoltageLink]) -> DigitalBidir:
  return DigitalBidir.from_supply(
    vss, vdd,
    voltage_limit_abs=(-0.5, 3.6),  # not referenced to Vdd!
    input_threshold_factor=(0.3, 0.7),
  )

def make_vl53l0x_i2d_model(vss: Port[VoltageLink], vdd: Port[VoltageLink]) -> I2cSlave:
  return I2cSlave(DigitalBidir.from_supply(
    vss, vdd,
    voltage_limit_abs=(-0.5, 3.6),  # not referenced to Vdd!
    input_threshold_abs=(0.6, 1.12),
  ))


class Vl53l0x_Device(DiscreteChip, JlcPart, FootprintBlock):
  def __init__(self) -> None:
    super().__init__()

    self.vdd = self.Port(VoltageSink(
      voltage_limits=(2.6, 3.5) * Volt,
      current_draw=(3, 40000) * uAmp  # up to 40mA including VCSEL when ranging
    ), [Power])
    self.vss = self.Port(Ground(), [Common])

    # TODO: the datasheet references values to IOVDD, but the value of IOVDD is never stated.
    # This model assumes that IOVDD = Vdd
    dio_model = DigitalBidir.from_supply(
      self.vss, self.vdd,
      voltage_limit_abs=(-0.5, 3.6),  # not referenced to Vdd!
      input_threshold_factor=(0.3, 0.7),
    )
    self.xshut = self.Port(DigitalSink.from_bidir(dio_model))
    self.gpio1 = self.Port(dio_model, optional=True)

    self.i2c = self.Port(I2cSlave(DigitalBidir.from_supply(
      self.vss, self.vdd,
      voltage_limit_abs=(-0.5, 3.6),  # not referenced to Vdd!
      input_threshold_abs=(0.6, 1.12),
    )), [Output])

  def contents(self):
    super().contents()
    self.footprint(
      'U', 'OptoDevice:ST_VL53L0X',
      {
        '1': self.vdd,  # AVddVcsel
        '2': self.vss,  # AVssVcsel
        '3': self.vss,  # GND
        '4': self.vss,  # GND2
        '5': self.xshut,
        '6': self.vss,  # GND3
        '7': self.gpio1,
        # '8': ,  # DNC, must be left floating
        '9': self.i2c.sda,
        '10': self.i2c.scl,
        '11': self.vdd,  # AVdd
        '12': self.vss,  # GND4
      },
      mfr='STMicroelectronics', part='VL53L0X',
      datasheet='https://www.st.com/content/ccc/resource/technical/document/datasheet/group3/b2/1e/33/77/c6/92/47/6b/DM00279086/files/DM00279086.pdf/jcr:content/translations/en.DM00279086.pdf'
    )
    self.assign(self.lcsc_part, "C91199")
    self.assign(self.actual_basic_part, False)


class Vl53l0xConnector(Block):
  """Connector to an external VL53L0X breakout board.
  Uses the pinout from the Adafruit product: https://www.adafruit.com/product/3317
  This has an onboard 2.8v regulator, but thankfully the IO tolerance is not referenced to Vdd"""
  def __init__(self) -> None:
    super().__init__()
    self.conn = self.Block(PassiveConnector(length=6))
    self.vdd = self.Export(self.conn.pins.allocate('1').adapt_to(VoltageSink(
      voltage_limits=(2.6, 3.5) * Volt,
      current_draw=(3, 40000) * uAmp  # up to 40mA including VCSEL when ranging
    )), [Power])
    self.vss = self.Export(self.conn.pins.allocate('2').adapt_to(Ground()), [Common])

    # TODO: the datasheet references values to IOVDD, but the value of IOVDD is never stated.
    # This model assumes that IOVDD = Vdd
    dio_model = DigitalBidir(
      voltage_limits=(-0.5, 3.6),  # not referenced to Vdd!
      current_draw=(0, 0),
      voltage_out=(0, self.vdd.link().voltage.lower()),  # TODO: assumed
      current_limits=Range.all(),  # TODO not given
      input_thresholds=(0.3 * self.vdd.link().voltage.upper(),
                        0.7 * self.vdd.link().voltage.upper()),
      output_thresholds=(0, self.vdd.link().voltage.upper()),
    )
    self.xshut = self.Export(self.conn.pins.allocate('6').adapt_to(
      DigitalSink.from_bidir(dio_model)
    ))
    self.gpio1 = self.Export(self.conn.pins.allocate('5').adapt_to(
      DigitalSource.from_bidir(dio_model)
    ), optional=True)

    i2c_model = DigitalBidir(
      voltage_limits=(-0.5, 3.6),  # not referenced to Vdd!
      current_draw=(0, 0),
      voltage_out=(0, self.vdd.link().voltage.lower()),  # TODO: assumed
      current_limits=Range.all(),  # TODO not given
      input_thresholds=(0.6, 1.12),  # this differs from the GPIO model
      output_thresholds=(0, self.vdd.link().voltage.upper()),
    )
    self.i2c = self.Port(I2cSlave.empty())
    self.connect(self.i2c.scl, self.conn.pins.allocate('3').adapt_to(i2c_model))
    self.connect(self.i2c.sda, self.conn.pins.allocate('4').adapt_to(i2c_model))


class Vl53l0x(Block):
  """Board-mount laser ToF sensor"""
  def __init__(self) -> None:
    super().__init__()

    self.ic = self.Block(Vl53l0x_Device())
    self.pwr = self.Export(self.ic.vdd, [Power])
    self.gnd = self.Export(self.ic.vss, [Common])

    self.i2c = self.Export(self.ic.i2c)
    self.xshut = self.Export(self.ic.xshut)  # MUST be driven
    self.gpio1 = self.Export(self.ic.gpio1, optional=True)

  def contents(self):
    super().contents()

    # Datasheet Figure 3, two decoupling capacitors
    self.vdd_cap = ElementDict[DecouplingCapacitor]()
    self.vdd_cap[0] = self.Block(DecouplingCapacitor(0.1 * uFarad(tol=0.2))).connected(self.gnd, self.pwr)
    self.vdd_cap[1] = self.Block(DecouplingCapacitor(4.7 * uFarad(tol=0.2))).connected(self.gnd, self.pwr)


class Vl53l0xArray(GeneratorBlock):
  """Array of Vl53l0x with common I2C but individually exposed XSHUT pins and optionally GPIO1."""
  @init_in_parent
  def __init__(self, count: IntLike):
    super().__init__()
    self.pwr = self.Port(VoltageSink.empty(), [Power])
    self.gnd = self.Port(Ground.empty(), [Common])
    self.i2c = self.Port(I2cSlave.empty())
    self.xshut = self.Port(Vector(DigitalSink.empty()))
    self.gpio1 = self.Port(Vector(DigitalBidir.empty()), optional=True)
    self.generator(self.generate, count)

  def generate(self, count: int):
    self.elt = ElementDict[Vl53l0x]()
    for elt_i in range(count):
      elt = self.elt[str(elt_i)] = self.Block(Vl53l0x())
      self.connect(self.pwr, elt.pwr)
      self.connect(self.gnd, elt.gnd)
      self.connect(self.i2c, elt.i2c)
      self.connect(self.xshut.append_elt(DigitalSink.empty(), str(elt_i)), elt.xshut)
      self.connect(self.gpio1.append_elt(DigitalBidir.empty(), str(elt_i)), elt.gpio1)
