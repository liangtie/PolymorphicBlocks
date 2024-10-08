"""Generated protocol buffer code."""
from google.protobuf.internal import builder as _builder
from google.protobuf import descriptor as _descriptor
from google.protobuf import descriptor_pool as _descriptor_pool
from google.protobuf import symbol_database as _symbol_database
_sym_db = _symbol_database.Default()
from ..edgir import common_pb2 as edgir_dot_common__pb2
DESCRIPTOR = _descriptor_pool.Default().AddSerializedFile(b'\n\x0fedgir/lit.proto\x12\tedgir.lit\x1a\x12edgir/common.proto"\x17\n\x08FloatLit\x12\x0b\n\x03val\x18\x01 \x01(\x01"\x15\n\x06IntLit\x12\x0b\n\x03val\x18\x01 \x01(\x12"\x16\n\x07BoolLit\x12\x0b\n\x03val\x18\x01 \x01(\x08"\x16\n\x07TextLit\x12\x0b\n\x03val\x18\x01 \x01(\t"V\n\x08RangeLit\x12$\n\x07minimum\x18\x01 \x01(\x0b2\x13.edgir.lit.ValueLit\x12$\n\x07maximum\x18\x02 \x01(\x0b2\x13.edgir.lit.ValueLit"\x84\x01\n\tStructLit\x122\n\x07members\x18\x01 \x03(\x0b2!.edgir.lit.StructLit.MembersEntry\x1aC\n\x0cMembersEntry\x12\x0b\n\x03key\x18\x01 \x01(\t\x12"\n\x05value\x18\x02 \x01(\x0b2\x13.edgir.lit.ValueLit:\x028\x01"-\n\x08ArrayLit\x12!\n\x04elts\x18\x01 \x03(\x0b2\x13.edgir.lit.ValueLit"\xc6\x02\n\x08ValueLit\x12\'\n\x08floating\x18\x01 \x01(\x0b2\x13.edgir.lit.FloatLitH\x00\x12$\n\x07integer\x18\x02 \x01(\x0b2\x11.edgir.lit.IntLitH\x00\x12%\n\x07boolean\x18\x03 \x01(\x0b2\x12.edgir.lit.BoolLitH\x00\x12"\n\x04text\x18\x04 \x01(\x0b2\x12.edgir.lit.TextLitH\x00\x12&\n\x06struct\x18\t \x01(\x0b2\x14.edgir.lit.StructLitH\x00\x12$\n\x05range\x18\n \x01(\x0b2\x13.edgir.lit.RangeLitH\x00\x12$\n\x05array\x18\x0b \x01(\x0b2\x13.edgir.lit.ArrayLitH\x00\x12$\n\x04meta\x18\x7f \x01(\x0b2\x16.edgir.common.MetadataB\x06\n\x04typeb\x06proto3')
_builder.BuildMessageAndEnumDescriptors(DESCRIPTOR, globals())
_builder.BuildTopDescriptorsAndMessages(DESCRIPTOR, 'edgir.lit_pb2', globals())
if _descriptor._USE_C_DESCRIPTORS == False:
    DESCRIPTOR._options = None
    _STRUCTLIT_MEMBERSENTRY._options = None
    _STRUCTLIT_MEMBERSENTRY._serialized_options = b'8\x01'
    _FLOATLIT._serialized_start = 50
    _FLOATLIT._serialized_end = 73
    _INTLIT._serialized_start = 75
    _INTLIT._serialized_end = 96
    _BOOLLIT._serialized_start = 98
    _BOOLLIT._serialized_end = 120
    _TEXTLIT._serialized_start = 122
    _TEXTLIT._serialized_end = 144
    _RANGELIT._serialized_start = 146
    _RANGELIT._serialized_end = 232
    _STRUCTLIT._serialized_start = 235
    _STRUCTLIT._serialized_end = 367
    _STRUCTLIT_MEMBERSENTRY._serialized_start = 300
    _STRUCTLIT_MEMBERSENTRY._serialized_end = 367
    _ARRAYLIT._serialized_start = 369
    _ARRAYLIT._serialized_end = 414
    _VALUELIT._serialized_start = 417
    _VALUELIT._serialized_end = 743