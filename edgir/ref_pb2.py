# -*- coding: utf-8 -*-
# Generated by the protocol buffer compiler.  DO NOT EDIT!
# source: edgir/ref.proto
"""Generated protocol buffer code."""
from google.protobuf.internal import enum_type_wrapper
from google.protobuf import descriptor as _descriptor
from google.protobuf import descriptor_pool as _descriptor_pool
from google.protobuf import message as _message
from google.protobuf import reflection as _reflection
from google.protobuf import symbol_database as _symbol_database
# @@protoc_insertion_point(imports)

_sym_db = _symbol_database.Default()


from edgir import common_pb2 as edgir_dot_common__pb2
from edgir import name_pb2 as edgir_dot_name__pb2


DESCRIPTOR = _descriptor_pool.Default().AddSerializedFile(b'\n\x0f\x65\x64gir/ref.proto\x12\tedgir.ref\x1a\x12\x65\x64gir/common.proto\x1a\x10\x65\x64gir/name.proto\"R\n\tLocalStep\x12-\n\x0ereserved_param\x18\x01 \x01(\x0e\x32\x13.edgir.ref.ReservedH\x00\x12\x0e\n\x04name\x18\x03 \x01(\tH\x00\x42\x06\n\x04step\"W\n\tLocalPath\x12#\n\x05steps\x18\x01 \x03(\x0b\x32\x14.edgir.ref.LocalStep\x12%\n\x04meta\x18\xff\x01 \x01(\x0b\x32\x16.edgir.common.Metadata\"\xa8\x01\n\x0bLibraryPath\x12&\n\x05start\x18\x01 \x01(\x0b\x32\x17.edgir.name.LibraryName\x12$\n\x05steps\x18\x02 \x03(\x0b\x32\x15.edgir.name.Namespace\x12$\n\x06target\x18\x03 \x01(\x0b\x32\x14.edgir.ref.LocalStep\x12%\n\x04meta\x18\xff\x01 \x01(\x0b\x32\x16.edgir.common.Metadata*c\n\x08Reserved\x12\r\n\tUNDEFINED\x10\x00\x12\x12\n\x0e\x43ONNECTED_LINK\x10\x01\x12\x10\n\x0cIS_CONNECTED\x10(\x12\n\n\x06LENGTH\x10*\x12\x0c\n\x08\x41LLOCATE\x10+\x12\x08\n\x04NAME\x10,b\x06proto3')

_RESERVED = DESCRIPTOR.enum_types_by_name['Reserved']
Reserved = enum_type_wrapper.EnumTypeWrapper(_RESERVED)
UNDEFINED = 0
CONNECTED_LINK = 1
IS_CONNECTED = 40
LENGTH = 42
ALLOCATE = 43
NAME = 44


_LOCALSTEP = DESCRIPTOR.message_types_by_name['LocalStep']
_LOCALPATH = DESCRIPTOR.message_types_by_name['LocalPath']
_LIBRARYPATH = DESCRIPTOR.message_types_by_name['LibraryPath']
LocalStep = _reflection.GeneratedProtocolMessageType('LocalStep', (_message.Message,), {
  'DESCRIPTOR' : _LOCALSTEP,
  '__module__' : 'edgir.ref_pb2'
  # @@protoc_insertion_point(class_scope:edgir.ref.LocalStep)
  })
_sym_db.RegisterMessage(LocalStep)

LocalPath = _reflection.GeneratedProtocolMessageType('LocalPath', (_message.Message,), {
  'DESCRIPTOR' : _LOCALPATH,
  '__module__' : 'edgir.ref_pb2'
  # @@protoc_insertion_point(class_scope:edgir.ref.LocalPath)
  })
_sym_db.RegisterMessage(LocalPath)

LibraryPath = _reflection.GeneratedProtocolMessageType('LibraryPath', (_message.Message,), {
  'DESCRIPTOR' : _LIBRARYPATH,
  '__module__' : 'edgir.ref_pb2'
  # @@protoc_insertion_point(class_scope:edgir.ref.LibraryPath)
  })
_sym_db.RegisterMessage(LibraryPath)

if _descriptor._USE_C_DESCRIPTORS == False:

  DESCRIPTOR._options = None
  _RESERVED._serialized_start=412
  _RESERVED._serialized_end=511
  _LOCALSTEP._serialized_start=68
  _LOCALSTEP._serialized_end=150
  _LOCALPATH._serialized_start=152
  _LOCALPATH._serialized_end=239
  _LIBRARYPATH._serialized_start=242
  _LIBRARYPATH._serialized_end=410
# @@protoc_insertion_point(module_scope)