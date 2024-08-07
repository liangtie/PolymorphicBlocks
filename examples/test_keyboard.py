"""
Mechanical keyboard example design.

Relies on footprints from external libraries.
In the KiCad Plugin and Content Manager, install the Keyswitch Kicad Library,
also on GitHub here https://github.com/perigoso/keyswitch-kicad-library
The project is set up to reference the third party library as installed by the KiCad
Plugin Manager, it does not need to be in your global library table.
"""

import unittest

from edg import *


class Keyboard(SimpleBoardTop):
  def contents(self) -> None:
    super().contents()

    self.usb = self.Block(UsbCReceptacle())
    self.reg = self.Block(Ldl1117(3.3*Volt(tol=0.05)))
    self.connect(self.usb.gnd, self.reg.gnd)
    self.connect(self.usb.pwr, self.reg.pwr_in)

    with self.implicit_connect(
            ImplicitConnect(self.reg.pwr_out, [Power]),
            ImplicitConnect(self.reg.gnd, [Common]),
    ) as imp:
      self.mcu = imp.Block(Stm32f103_48())
      self.connect(self.usb.usb, self.mcu.usb.request())

      self.sw = self.Block(SwitchMatrix(nrows=3, ncols=2))
      self.connect(self.sw.cols, self.mcu.gpio.request_vector())
      self.connect(self.sw.rows, self.mcu.gpio.request_vector())

  def refinements(self) -> Refinements:
    return super().refinements() + Refinements(
      class_refinements=[
        (Switch, KailhSocket),
      ],
    )


class KeyboardTestCase(unittest.TestCase):
  def test_design(self) -> None:
    compile_board_inplace(Keyboard)
