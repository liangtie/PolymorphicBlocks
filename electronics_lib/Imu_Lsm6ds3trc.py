from electronics_abstract_parts import *


class Imu_Lsm6ds3trc_Device(DiscreteChip, FootprintBlock):
    def __init__(self) -> None:
        super().__init__()
        self.vdd = self.Port(VoltageSink(
            voltage_limits=(1.71, 3.6)*Volt,
            current_draw=(0.29, 0.9)*mAmp  # typical values for low-power and high-performance modes
        ))
        self.vddio = self.Port(VoltageSink(
            voltage_limits=(1.62*Volt, self.vdd.voltage_limits.upper()+0.1)
        ))
        self.gnd = self.Port(Ground())

        # TODO figure out how to initialize pins
        din_model = DigitalBidir.from_supply(
            self.gnd, self.vdd,
            current_limits=(-3, 0)*mAmp,
            input_threshold_abs=(0.3, self.vddio.voltage_limits.upper()+0.3)
        )
        self.i2c = self.Port(I2cSlave(din_model))

        dout_model = DigitalSingleSource.low_from_supply(self.gnd)
        self.int1 = self.Port(dout_model, optional=True)
        self.int2 = self.Port(dout_model, optional=True)

    def contents(self) -> None:
        self.footprint(
            'U', 'Package_LGA:LGA-14_3x2.5mm_P0.5mm_LayoutBorder3x4y',
            {
                '1': self.gnd,  # sa0
                '2': self.gnd,  # not used in mode 1
                '3': self.gnd,  # not used in mode 1
                '4': self.int1,
                '5': self.vddio,
                '6': self.gnd,
                '7': self.gnd,
                '8': self.vdd,
                '9': self.int2,
                # '10': self.nc,  # leave unconnected
                # '11': self.nc,  # leave unconnected
                '12': self.vddio,  # cs pin
                '13': self.i2c.scl,
                '14': self.i2c.sda,
            },
            mfr='STMicroelectronics', part='LSM6DS3TR-C',
            datasheet='https://datasheet.lcsc.com/lcsc/2102261805_STMicroelectronics-LSM6DS3TR-C_C967633.pdf'
        )


class Imu_Lsm6ds3trc(Block):
    def __init__(self):
        super().__init__()
        self.ic = self.Block(Imu_Lsm6ds3trc_Device())
        self.vdd = self.Export(self.ic.vdd, [Power])
        self.vddio = self.Export(self.ic.vddio, [Power])
        self.gnd = self.Export(self.ic.gnd, [Common])
        self.i2c = self.Export(self.ic.i2c)
        self.int1 = self.Export(self.ic.int1, optional=True)
        self.int2 = self.Export(self.ic.int2, optional=True)

    def contents(self):
        super().contents()
        self.vdd_cap = self.Block(DecouplingCapacitor(100*nFarad(tol=0.1))).connected(self.gnd, self.ic.vdd)
        self.vddio_cap = self.Block(DecouplingCapacitor(100*nFarad(tol=0.1))).connected(self.gnd, self.ic.vddio)
