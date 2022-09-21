from electronics_lib import *


class BaseBoardTop(DesignTop):
  """Design top with refinements for intermediate-level (0603+ SMD), hand-solderable components."""
  def refinements(self) -> Refinements:
    return super().refinements() + Refinements(
      class_refinements=[
        (Resistor, GenericChipResistor),
        (Capacitor, GenericMlcc),
        (Inductor, DigikeyInductor),
        (Switch, SmtSwitch),
        (Diode, DigikeySmtDiode),
        (Led, SmtLed),
        (RgbLedCommonAnode, SmtRgbLed),
        (ZenerDiode, DigikeySmtZenerDiode),
        (Fet, DigikeyFet),
        (SwitchFet, DigikeySwitchFet),
        (Crystal, DigikeyCrystal),

        (IndicatorSinkLed, IndicatorSinkLedResistor),

        (Fpc050, HiroseFh12sh),
        (UsbEsdDiode, Tpd2e009),
        (TestPoint, TeRc),

        (SwdCortexTargetWithTdiConnector, SwdCortexTargetHeader),

        (Vl53l0x, Vl53l0xApplication)
      ],
    )


class BoardTop(BaseBoardTop):
  pass


class JlcToolingHoles(Block):
  def contents(self):
    super().contents()
    self.th1 = self.Block(JlcToolingHole())
    self.th2 = self.Block(JlcToolingHole())
    self.th3 = self.Block(JlcToolingHole())


class JlcBoardTop(BaseBoardTop):
  """Design top with refinements to use parts from JLC's assembly service and including the tooling holes"""
  def contents(self):
    super().contents()
    self.jlc_th = self.Block(JlcToolingHoles())

  def refinements(self) -> Refinements:
    return super().refinements() + Refinements(
      class_refinements=[
        (Resistor, JlcResistor),
        (Capacitor, JlcCapacitor),
        (Inductor, JlcInductor),
        (ResistorArray, JlcResistorArray),
        (Crystal, JlcCrystal),

        (Switch, JlcSwitch),
        (Led, JlcLed),
        (ZenerDiode, JlcZenerDiode),
        (Diode, JlcDiode),
        (Fet, JlcFet),

        (UsbEsdDiode, Esda5v3l),
        (Opamp, Lmv321),
        (TestPoint, Keystone5015),  # this is larger, but is part of JLC's parts inventory
      ],
      class_values=[  # realistically only RCs are going to likely be basic parts
        (JlcResistor, ['require_basic_part'], True),
        (JlcCapacitor, ['require_basic_part'], True),
      ],
    )
