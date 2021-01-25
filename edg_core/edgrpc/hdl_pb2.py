# Generated by the protocol buffer compiler.  DO NOT EDIT!
# source: hdl.proto

import sys
_b=sys.version_info[0]<3 and (lambda x:x) or (lambda x:x.encode('latin1'))
from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from google.protobuf import reflection as _reflection
from google.protobuf import symbol_database as _symbol_database
from google.protobuf import descriptor_pb2
# @@protoc_insertion_point(imports)

_sym_db = _symbol_database.Default()


from edg_core.edgir import schema_pb2 as schema__pb2
from edg_core.edgir import ref_pb2 as ref__pb2
from edg_core.edgir import elem_pb2 as elem__pb2
from edg_core.edgir import lit_pb2 as lit__pb2


DESCRIPTOR = _descriptor.FileDescriptor(
  name='hdl.proto',
  package='edg.compiler',
  syntax='proto3',
  serialized_pb=_b('\n\thdl.proto\x12\x0c\x65\x64g.compiler\x1a\x0cschema.proto\x1a\tref.proto\x1a\nelem.proto\x1a\tlit.proto\"\x1a\n\nModuleName\x12\x0c\n\x04name\x18\x01 \x01(\t\"\xba\x01\n\x10GeneratorRequest\x12#\n\x05\x63lass\x18\x01 \x01(\x0b\x32\x14.edg.ref.LibraryPath\x12\x34\n\x06values\x18\x02 \x03(\x0b\x32$.edg.compiler.GeneratorRequest.Value\x1aK\n\x05Value\x12 \n\x04path\x18\x01 \x01(\x0b\x32\x12.edg.ref.LocalPath\x12 \n\x05value\x18\x02 \x01(\x0b\x32\x11.edg.lit.ValueLit2\xf8\x01\n\x0cHdlInterface\x12M\n\x17LibraryElementsInModule\x12\x18.edg.compiler.ModuleName\x1a\x14.edg.ref.LibraryPath\"\x00\x30\x01\x12G\n\x11GetLibraryElement\x12\x14.edg.ref.LibraryPath\x1a\x1a.edg.schema.Library.NS.Val\"\x00\x12P\n\x12\x45laborateGenerator\x12\x1e.edg.compiler.GeneratorRequest\x1a\x18.edg.elem.HierarchyBlock\"\x00\x62\x06proto3')
  ,
  dependencies=[schema__pb2.DESCRIPTOR,ref__pb2.DESCRIPTOR,elem__pb2.DESCRIPTOR,lit__pb2.DESCRIPTOR,])
_sym_db.RegisterFileDescriptor(DESCRIPTOR)




_MODULENAME = _descriptor.Descriptor(
  name='ModuleName',
  full_name='edg.compiler.ModuleName',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='name', full_name='edg.compiler.ModuleName.name', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=75,
  serialized_end=101,
)


_GENERATORREQUEST_VALUE = _descriptor.Descriptor(
  name='Value',
  full_name='edg.compiler.GeneratorRequest.Value',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='path', full_name='edg.compiler.GeneratorRequest.Value.path', index=0,
      number=1, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    _descriptor.FieldDescriptor(
      name='value', full_name='edg.compiler.GeneratorRequest.Value.value', index=1,
      number=2, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=215,
  serialized_end=290,
)

_GENERATORREQUEST = _descriptor.Descriptor(
  name='GeneratorRequest',
  full_name='edg.compiler.GeneratorRequest',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='class', full_name='edg.compiler.GeneratorRequest.class', index=0,
      number=1, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    _descriptor.FieldDescriptor(
      name='values', full_name='edg.compiler.GeneratorRequest.values', index=1,
      number=2, type=11, cpp_type=10, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
  ],
  extensions=[
  ],
  nested_types=[_GENERATORREQUEST_VALUE, ],
  enum_types=[
  ],
  options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=104,
  serialized_end=290,
)

_GENERATORREQUEST_VALUE.fields_by_name['path'].message_type = ref__pb2._LOCALPATH
_GENERATORREQUEST_VALUE.fields_by_name['value'].message_type = lit__pb2._VALUELIT
_GENERATORREQUEST_VALUE.containing_type = _GENERATORREQUEST
_GENERATORREQUEST.fields_by_name['class'].message_type = ref__pb2._LIBRARYPATH
_GENERATORREQUEST.fields_by_name['values'].message_type = _GENERATORREQUEST_VALUE
DESCRIPTOR.message_types_by_name['ModuleName'] = _MODULENAME
DESCRIPTOR.message_types_by_name['GeneratorRequest'] = _GENERATORREQUEST

ModuleName = _reflection.GeneratedProtocolMessageType('ModuleName', (_message.Message,), dict(
  DESCRIPTOR = _MODULENAME,
  __module__ = 'hdl_pb2'
  # @@protoc_insertion_point(class_scope:edg.compiler.ModuleName)
  ))
_sym_db.RegisterMessage(ModuleName)

GeneratorRequest = _reflection.GeneratedProtocolMessageType('GeneratorRequest', (_message.Message,), dict(

  Value = _reflection.GeneratedProtocolMessageType('Value', (_message.Message,), dict(
    DESCRIPTOR = _GENERATORREQUEST_VALUE,
    __module__ = 'hdl_pb2'
    # @@protoc_insertion_point(class_scope:edg.compiler.GeneratorRequest.Value)
    ))
  ,
  DESCRIPTOR = _GENERATORREQUEST,
  __module__ = 'hdl_pb2'
  # @@protoc_insertion_point(class_scope:edg.compiler.GeneratorRequest)
  ))
_sym_db.RegisterMessage(GeneratorRequest)
_sym_db.RegisterMessage(GeneratorRequest.Value)


# @@protoc_insertion_point(module_scope)
