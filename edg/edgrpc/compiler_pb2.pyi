"""
@generated by mypy-protobuf.  Do not edit manually!
isort:skip_file
* Defines messages for a service that provides EDG compilation services.
Interface to the HDL (eg, library fetch) is not included here.

This no longer uses gRPC to avoid complexity of sockets.
"""
import builtins
import collections.abc
from .. import edgir
from .. import edgrpc
import google.protobuf.descriptor
import google.protobuf.internal.containers
import google.protobuf.message
import sys
if sys.version_info >= (3, 8):
    import typing as typing_extensions
else:
    import typing_extensions
DESCRIPTOR: google.protobuf.descriptor.FileDescriptor

@typing_extensions.final
class ErrorRecord(google.protobuf.message.Message):
    DESCRIPTOR: google.protobuf.descriptor.Descriptor
    PATH_FIELD_NUMBER: builtins.int
    KIND_FIELD_NUMBER: builtins.int
    NAME_FIELD_NUMBER: builtins.int
    DETAILS_FIELD_NUMBER: builtins.int

    @property
    def path(self) -> edgir.ref_pb2.LocalPath:
        """link / block / port, cannot be the constraint"""
    kind: builtins.str
    'kind of error, eg failed to generate'
    name: builtins.str
    'constraint name / short description'
    details: builtins.str
    'longer description, optional'

    def __init__(self, *, path: edgir.ref_pb2.LocalPath | None=..., kind: builtins.str=..., name: builtins.str=..., details: builtins.str=...) -> None:
        ...

    def HasField(self, field_name: typing_extensions.Literal['path', b'path']) -> builtins.bool:
        ...

    def ClearField(self, field_name: typing_extensions.Literal['details', b'details', 'kind', b'kind', 'name', b'name', 'path', b'path']) -> None:
        ...
global___ErrorRecord = ErrorRecord

@typing_extensions.final
class CompilerRequest(google.protobuf.message.Message):
    DESCRIPTOR: google.protobuf.descriptor.Descriptor
    DESIGN_FIELD_NUMBER: builtins.int
    REFINEMENTS_FIELD_NUMBER: builtins.int

    @property
    def design(self) -> edgir.schema_pb2.Design:
        ...

    @property
    def refinements(self) -> edgrpc.hdl_pb2.Refinements:
        ...

    def __init__(self, *, design: edgir.schema_pb2.Design | None=..., refinements: edgrpc.hdl_pb2.Refinements | None=...) -> None:
        ...

    def HasField(self, field_name: typing_extensions.Literal['design', b'design', 'refinements', b'refinements']) -> builtins.bool:
        ...

    def ClearField(self, field_name: typing_extensions.Literal['design', b'design', 'refinements', b'refinements']) -> None:
        ...
global___CompilerRequest = CompilerRequest

@typing_extensions.final
class CompilerResult(google.protobuf.message.Message):
    DESCRIPTOR: google.protobuf.descriptor.Descriptor

    @typing_extensions.final
    class Value(google.protobuf.message.Message):
        DESCRIPTOR: google.protobuf.descriptor.Descriptor
        PATH_FIELD_NUMBER: builtins.int
        VALUE_FIELD_NUMBER: builtins.int

        @property
        def path(self) -> edgir.ref_pb2.LocalPath:
            ...

        @property
        def value(self) -> edgir.lit_pb2.ValueLit:
            ...

        def __init__(self, *, path: edgir.ref_pb2.LocalPath | None=..., value: edgir.lit_pb2.ValueLit | None=...) -> None:
            ...

        def HasField(self, field_name: typing_extensions.Literal['path', b'path', 'value', b'value']) -> builtins.bool:
            ...

        def ClearField(self, field_name: typing_extensions.Literal['path', b'path', 'value', b'value']) -> None:
            ...
    DESIGN_FIELD_NUMBER: builtins.int
    ERRORS_FIELD_NUMBER: builtins.int
    SOLVEDVALUES_FIELD_NUMBER: builtins.int

    @property
    def design(self) -> edgir.schema_pb2.Design:
        ...

    @property
    def errors(self) -> google.protobuf.internal.containers.RepeatedCompositeFieldContainer[global___ErrorRecord]:
        ...

    @property
    def solvedValues(self) -> google.protobuf.internal.containers.RepeatedCompositeFieldContainer[global___CompilerResult.Value]:
        ...

    def __init__(self, *, design: edgir.schema_pb2.Design | None=..., errors: collections.abc.Iterable[global___ErrorRecord] | None=..., solvedValues: collections.abc.Iterable[global___CompilerResult.Value] | None=...) -> None:
        ...

    def HasField(self, field_name: typing_extensions.Literal['design', b'design']) -> builtins.bool:
        ...

    def ClearField(self, field_name: typing_extensions.Literal['design', b'design', 'errors', b'errors', 'solvedValues', b'solvedValues']) -> None:
        ...
global___CompilerResult = CompilerResult