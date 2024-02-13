from edg import *  # Importing all from the 'edg' library for electronics design

from typing import Dict, Type

VOLTAGE_LIMIT = 'voltage_limits'
FOOTPRINT = 'footprint'
INA1x9_PARAM = dict(
    Ina139={VOLTAGE_LIMIT:(2.7, 40)*Volt,
            FOOTPRINT:dict(
                    refdes='U', footprint='Custom:SOT95P280X145-5N',
                    mfr='Texas Instruments', 
                    part='INA139',
                    datasheet='https://www.ti.com/product/INA139'
                )
            },
    Ina169={VOLTAGE_LIMIT:(2.7, 60)*Volt,
            FOOTPRINT:dict(
                refdes='U', footprint='Custom:SOT95P280X145-5N',
                mfr='Texas Instruments',
                part='INA169',
                datasheet='https://www.ti.com/product/INA169'
            )
            },
)

@non_library
class Ina1x9_Device(FootprintBlock):
    DEV_PARAM: Dict
    @init_in_parent
    def __init__(self) -> None:
        super().__init__()
        self.vin_plus = self.Port(AnalogSink(voltage_limits=self.DEV_PARAM[VOLTAGE_LIMIT]))
        self.vin_minus = self.Port(AnalogSink(voltage_limits=self.DEV_PARAM[VOLTAGE_LIMIT]))
        self.vout = self.Port(AnalogSource())
        self.vplus = self.Port(VoltageSink(voltage_limits=self.DEV_PARAM[VOLTAGE_LIMIT]))
        self.gnd = self.Port(Ground())
        
    def contents(self):
        super().contents()
        self.footprint(
        pinning=
        {
            '1': self.vout,
            '2': self.gnd,
            '3': self.vin_plus,
            '4': self.vin_minus,
            '5': self.vplus,
        },
        **self.DEV_PARAM[FOOTPRINT])

@non_library
class Ina1x9(Sensor, Block):
    DEVICE: Type[Ina1x9_Device]
    @init_in_parent
    def __init__(self, resistor_shunt: RangeLike, gain: FloatLike) -> None:
        super().__init__()
        # Instantiate the INA139 device
        self.ic = self.Block(self.DEVICE())
        self.Rs = self.Block(CurrentSenseResistor(resistance=resistor_shunt) )# 0.001Ohm -> 35A, 0.1Ohm -> 3.5A, 1Ohm -> 0.35A
        self.Rl = self.Block(Resistor(resistance=(gain * 0.95, gain * 1.05)))

        self.pwr_in = self.Export(self.Rs.pwr_in)
        self.pwr_out = self.Export(self.Rs.pwr_out)

        self.pwr_logic = self.Export(self.ic.vplus, [Power])
        self.gnd = self.Export(self.ic.gnd, [Common])
        self.out = self.Export(self.ic.vout)

    def contents(self):
        self.connect(self.ic.vin_plus, self.Rs.sense_in)
        self.connect(self.ic.vin_minus, self.Rs.sense_out)

        self.connect(self.Rl.a.adapt_to(AnalogSink()), self.ic.vout)
        self.connect(self.Rl.b.adapt_to(VoltageSink()), self.gnd)

@non_library
class Ina1x9WithBuffer(Ina1x9):
    @init_in_parent
    def __init__(self, resistor_shunt: RangeLike, gain: FloatLike) -> None:
        # Instantiate the INA139 device
        super().__init__(resistor_shunt, gain)
        self.opa = self.Block(Opamp())
        self.out = self.Export(self.opa.out)
        self.inn = self.Export(self.opa.inn)


    def contents(self):
        super().contents()
        self.connect(self.opa.pwr, self.ic.vplus)
        self.connect(self.opa.gnd, self.ic.gnd)
        self.connect(self.opa.inp, self.ic.vout)


class Ina139_Device(Ina1x9_Device):
    DEV_PARAM = INA1x9_PARAM['Ina139']


class Ina169_Device(Ina1x9_Device):
    DEV_PARAM = INA1x9_PARAM['Ina169']


class Ina139(Ina1x9):
    DEVICE = Ina139_Device


class Ina169(Ina1x9):
    DEVICE = Ina169_Device


class Ina139WithBuffer(Ina1x9WithBuffer):
    DEVICE = Ina139_Device


# class Ina139WithBuffer(Sensor, Block):
#     @init_in_parent
#     def __init__(self, resistor_shunt: RangeLike, gain: FloatLike) -> None:
#         super().__init__()
#         # Instantiate the INA139 device
#         self.ic = self.Block(Ina139_Device())
#         self.Rs = self.Block(CurrentSenseResistor(resistance=resistor_shunt) )# 0.001Ohm -> 35A, 0.1Ohm -> 3.5A, 1Ohm -> 0.35A
#         self.Rl = self.Block(Resistor(resistance=(gain * 0.95, gain * 1.05)))
#         self.opa = self.Block(Opamp())
#
#         self.vin_plus = self.Export(self.Rs.pwr_in)
#         self.vin_minus = self.Export(self.Rs.pwr_out)
#
#         self.vplus = self.Export(self.ic.vplus, [Power])
#         self.gnd = self.Export(self.ic.gnd, [Common])
#         self.out = self.Export(self.opa.out)
#         self.inn = self.Export(self.opa.inn)
#
#     def contents(self):
#         self.connect(self.ic.vin_plus, self.Rs.sense_in)
#         self.connect(self.ic.vin_minus, self.Rs.sense_out)
#
#         self.connect(self.Rl.a.adapt_to(AnalogSink()), self.ic.vout)
#         self.connect(self.Rl.b.adapt_to(VoltageSink()), self.gnd)
#
#         self.connect(self.opa.pwr, self.ic.vplus)
#         self.connect(self.opa.gnd, self.ic.gnd)
#         # self.connect(self.opa.inn), self.gnd.as_analog_source()
#         self.connect(self.opa.inp, self.ic.vout)


