# -*- coding: utf-8 -*-
# Generated by the protocol buffer compiler.  DO NOT EDIT!
# source: edgrpc/hdl.proto
"""Generated protocol buffer code."""
from google.protobuf.internal import builder as _builder
from google.protobuf import descriptor as _descriptor
from google.protobuf import descriptor_pool as _descriptor_pool
from google.protobuf import symbol_database as _symbol_database
# @@protoc_insertion_point(imports)

_sym_db = _symbol_database.Default()


from edgir import schema_pb2 as edgir_dot_schema__pb2
from edgir import ref_pb2 as edgir_dot_ref__pb2
from edgir import elem_pb2 as edgir_dot_elem__pb2
from edgir import lit_pb2 as edgir_dot_lit__pb2


DESCRIPTOR = _descriptor_pool.Default().AddSerializedFile(b'\n\x10\x65\x64grpc/hdl.proto\x12\nedgrpc.hdl\x1a\x12\x65\x64gir/schema.proto\x1a\x0f\x65\x64gir/ref.proto\x1a\x10\x65\x64gir/elem.proto\x1a\x0f\x65\x64gir/lit.proto\"\x85\x04\n\x0bRefinements\x12\x34\n\nsubclasses\x18\x01 \x03(\x0b\x32 .edgrpc.hdl.Refinements.Subclass\x12-\n\x06values\x18\x02 \x03(\x0b\x32\x1d.edgrpc.hdl.Refinements.Value\x1a\x8e\x01\n\x08Subclass\x12$\n\x04path\x18\x01 \x01(\x0b\x32\x14.edgir.ref.LocalPathH\x00\x12%\n\x03\x63ls\x18\x02 \x01(\x0b\x32\x16.edgir.ref.LibraryPathH\x00\x12+\n\x0breplacement\x18\x03 \x01(\x0b\x32\x16.edgir.ref.LibraryPathB\x08\n\x06source\x1a\xff\x01\n\x05Value\x12$\n\x04path\x18\x01 \x01(\x0b\x32\x14.edgir.ref.LocalPathH\x00\x12\x41\n\tcls_param\x18\x02 \x01(\x0b\x32,.edgrpc.hdl.Refinements.Value.ClassParamPathH\x00\x12\"\n\x05value\x18\x03 \x01(\x0b\x32\x13.edgir.lit.ValueLit\x1a_\n\x0e\x43lassParamPath\x12#\n\x03\x63ls\x18\x01 \x01(\x0b\x32\x16.edgir.ref.LibraryPath\x12(\n\nparam_path\x18\x02 \x01(\x0b\x32\x14.edgir.ref.LocalPathB\x08\n\x06source\"\x1a\n\nModuleName\x12\x0c\n\x04name\x18\x01 \x01(\t\"8\n\rIndexResponse\x12\'\n\x07indexed\x18\x01 \x03(\x0b\x32\x16.edgir.ref.LibraryPath\"9\n\x0eLibraryRequest\x12\'\n\x07\x65lement\x18\x02 \x01(\x0b\x32\x16.edgir.ref.LibraryPath\"n\n\x0fLibraryResponse\x12-\n\x07\x65lement\x18\x01 \x01(\x0b\x32\x1c.edgir.schema.Library.NS.Val\x12,\n\x0brefinements\x18\x03 \x01(\x0b\x32\x17.edgrpc.hdl.Refinements\"\xc0\x01\n\x10GeneratorRequest\x12\'\n\x07\x65lement\x18\x02 \x01(\x0b\x32\x16.edgir.ref.LibraryPath\x12\x32\n\x06values\x18\x04 \x03(\x0b\x32\".edgrpc.hdl.GeneratorRequest.Value\x1aO\n\x05Value\x12\"\n\x04path\x18\x01 \x01(\x0b\x32\x14.edgir.ref.LocalPath\x12\"\n\x05value\x18\x02 \x01(\x0b\x32\x13.edgir.lit.ValueLit\"B\n\x11GeneratorResponse\x12-\n\tgenerated\x18\x01 \x01(\x0b\x32\x1a.edgir.elem.HierarchyBlock\"\xe8\x01\n\x0e\x42\x61\x63kendRequest\x12\'\n\x07\x62\x61\x63kend\x18\x01 \x01(\x0b\x32\x16.edgir.ref.LibraryPath\x12$\n\x06\x64\x65sign\x18\x02 \x01(\x0b\x32\x14.edgir.schema.Design\x12\x36\n\x0csolvedValues\x18\x03 \x03(\x0b\x32 .edgrpc.hdl.BackendRequest.Value\x1aO\n\x05Value\x12\"\n\x04path\x18\x01 \x01(\x0b\x32\x14.edgir.ref.LocalPath\x12\"\n\x05value\x18\x02 \x01(\x0b\x32\x13.edgir.lit.ValueLit\"\x8e\x01\n\x0f\x42\x61\x63kendResponse\x12\x33\n\x07results\x18\x01 \x03(\x0b\x32\".edgrpc.hdl.BackendResponse.Result\x1a\x46\n\x06Result\x12\"\n\x04path\x18\x01 \x01(\x0b\x32\x14.edgir.ref.LocalPath\x12\x0e\n\x04text\x18\x02 \x01(\tH\x00\x42\x08\n\x06result\"1\n\rErrorResponse\x12\r\n\x05\x65rror\x18\x01 \x01(\t\x12\x11\n\ttraceback\x18\x02 \x01(\t\"\xf2\x01\n\nHdlRequest\x12.\n\x0cindex_module\x18\x01 \x01(\x0b\x32\x16.edgrpc.hdl.ModuleNameH\x00\x12\x39\n\x13get_library_element\x18\x02 \x01(\x0b\x32\x1a.edgrpc.hdl.LibraryRequestH\x00\x12;\n\x13\x65laborate_generator\x18\x03 \x01(\x0b\x32\x1c.edgrpc.hdl.GeneratorRequestH\x00\x12\x31\n\x0brun_backend\x18\x04 \x01(\x0b\x32\x1a.edgrpc.hdl.BackendRequestH\x00\x42\t\n\x07request\"\xa6\x02\n\x0bHdlResponse\x12\x31\n\x0cindex_module\x18\x01 \x01(\x0b\x32\x19.edgrpc.hdl.IndexResponseH\x00\x12:\n\x13get_library_element\x18\x02 \x01(\x0b\x32\x1b.edgrpc.hdl.LibraryResponseH\x00\x12<\n\x13\x65laborate_generator\x18\x03 \x01(\x0b\x32\x1d.edgrpc.hdl.GeneratorResponseH\x00\x12\x32\n\x0brun_backend\x18\x04 \x01(\x0b\x32\x1b.edgrpc.hdl.BackendResponseH\x00\x12*\n\x05\x65rror\x18\x63 \x01(\x0b\x32\x19.edgrpc.hdl.ErrorResponseH\x00\x42\n\n\x08responseb\x06proto3')

_builder.BuildMessageAndEnumDescriptors(DESCRIPTOR, globals())
_builder.BuildTopDescriptorsAndMessages(DESCRIPTOR, 'edgrpc.hdl_pb2', globals())
if _descriptor._USE_C_DESCRIPTORS == False:

  DESCRIPTOR._options = None
  _REFINEMENTS._serialized_start=105
  _REFINEMENTS._serialized_end=622
  _REFINEMENTS_SUBCLASS._serialized_start=222
  _REFINEMENTS_SUBCLASS._serialized_end=364
  _REFINEMENTS_VALUE._serialized_start=367
  _REFINEMENTS_VALUE._serialized_end=622
  _REFINEMENTS_VALUE_CLASSPARAMPATH._serialized_start=517
  _REFINEMENTS_VALUE_CLASSPARAMPATH._serialized_end=612
  _MODULENAME._serialized_start=624
  _MODULENAME._serialized_end=650
  _INDEXRESPONSE._serialized_start=652
  _INDEXRESPONSE._serialized_end=708
  _LIBRARYREQUEST._serialized_start=710
  _LIBRARYREQUEST._serialized_end=767
  _LIBRARYRESPONSE._serialized_start=769
  _LIBRARYRESPONSE._serialized_end=879
  _GENERATORREQUEST._serialized_start=882
  _GENERATORREQUEST._serialized_end=1074
  _GENERATORREQUEST_VALUE._serialized_start=995
  _GENERATORREQUEST_VALUE._serialized_end=1074
  _GENERATORRESPONSE._serialized_start=1076
  _GENERATORRESPONSE._serialized_end=1142
  _BACKENDREQUEST._serialized_start=1145
  _BACKENDREQUEST._serialized_end=1377
  _BACKENDREQUEST_VALUE._serialized_start=995
  _BACKENDREQUEST_VALUE._serialized_end=1074
  _BACKENDRESPONSE._serialized_start=1380
  _BACKENDRESPONSE._serialized_end=1522
  _BACKENDRESPONSE_RESULT._serialized_start=1452
  _BACKENDRESPONSE_RESULT._serialized_end=1522
  _ERRORRESPONSE._serialized_start=1524
  _ERRORRESPONSE._serialized_end=1573
  _HDLREQUEST._serialized_start=1576
  _HDLREQUEST._serialized_end=1818
  _HDLRESPONSE._serialized_start=1821
  _HDLRESPONSE._serialized_end=2115
# @@protoc_insertion_point(module_scope)
