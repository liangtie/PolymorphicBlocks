"""
@generated by mypy-protobuf.  Do not edit manually!
isort:skip_file
*
File : schema.proto
Package : edg.schema

These types contain the highest level data structures we use to
describe sets of blocks, ports, and links.
"""
import builtins
import collections.abc
import edgir.common_pb2
import edgir.elem_pb2
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
class Library(google.protobuf.message.Message):
    """* This is the top-level schema for a library of blocks, ports, and links.

    It need not be complete (containing all the blocks to be used in a design)
    or closed (containing enough information that every reference/inheritance
    can be resolved).

    It can be merged with other libraries when there are no namespace collisions
    or definitional conflicts. This means that we can shuffle around partial
    libraries, for merging, modification, etc..
    """

    DESCRIPTOR: google.protobuf.descriptor.Descriptor

    @typing_extensions.final
    class NS(google.protobuf.message.Message):
        """* Library Namespace, avoiding collision w/ edg.name.Namespace"""

        DESCRIPTOR: google.protobuf.descriptor.Descriptor

        @typing_extensions.final
        class Val(google.protobuf.message.Message):
            DESCRIPTOR: google.protobuf.descriptor.Descriptor

            PORT_FIELD_NUMBER: builtins.int
            BUNDLE_FIELD_NUMBER: builtins.int
            HIERARCHY_BLOCK_FIELD_NUMBER: builtins.int
            LINK_FIELD_NUMBER: builtins.int
            NAMESPACE_FIELD_NUMBER: builtins.int
            @property
            def port(self) -> edgir.elem_pb2.Port: ...
            @property
            def bundle(self) -> edgir.elem_pb2.Bundle: ...
            @property
            def hierarchy_block(self) -> edgir.elem_pb2.HierarchyBlock: ...
            @property
            def link(self) -> edgir.elem_pb2.Link: ...
            @property
            def namespace(self) -> global___Library.NS: ...
            def __init__(
                self,
                *,
                port: edgir.elem_pb2.Port | None = ...,
                bundle: edgir.elem_pb2.Bundle | None = ...,
                hierarchy_block: edgir.elem_pb2.HierarchyBlock | None = ...,
                link: edgir.elem_pb2.Link | None = ...,
                namespace: global___Library.NS | None = ...,
            ) -> None: ...
            def HasField(self, field_name: typing_extensions.Literal["bundle", b"bundle", "hierarchy_block", b"hierarchy_block", "link", b"link", "namespace", b"namespace", "port", b"port", "type", b"type"]) -> builtins.bool: ...
            def ClearField(self, field_name: typing_extensions.Literal["bundle", b"bundle", "hierarchy_block", b"hierarchy_block", "link", b"link", "namespace", b"namespace", "port", b"port", "type", b"type"]) -> None: ...
            def WhichOneof(self, oneof_group: typing_extensions.Literal["type", b"type"]) -> typing_extensions.Literal["port", "bundle", "hierarchy_block", "link", "namespace"] | None: ...

        @typing_extensions.final
        class MembersEntry(google.protobuf.message.Message):
            DESCRIPTOR: google.protobuf.descriptor.Descriptor

            KEY_FIELD_NUMBER: builtins.int
            VALUE_FIELD_NUMBER: builtins.int
            key: builtins.str
            @property
            def value(self) -> global___Library.NS.Val: ...
            def __init__(
                self,
                *,
                key: builtins.str = ...,
                value: global___Library.NS.Val | None = ...,
            ) -> None: ...
            def HasField(self, field_name: typing_extensions.Literal["value", b"value"]) -> builtins.bool: ...
            def ClearField(self, field_name: typing_extensions.Literal["key", b"key", "value", b"value"]) -> None: ...

        MEMBERS_FIELD_NUMBER: builtins.int
        @property
        def members(self) -> google.protobuf.internal.containers.MessageMap[builtins.str, global___Library.NS.Val]: ...
        def __init__(
            self,
            *,
            members: collections.abc.Mapping[builtins.str, global___Library.NS.Val] | None = ...,
        ) -> None: ...
        def ClearField(self, field_name: typing_extensions.Literal["members", b"members"]) -> None: ...

    @typing_extensions.final
    class LibIdent(google.protobuf.message.Message):
        """* How we identify a library within a set. Will probably
        evolve to capture more metadata.
        """

        DESCRIPTOR: google.protobuf.descriptor.Descriptor

        NAME_FIELD_NUMBER: builtins.int
        name: builtins.str
        def __init__(
            self,
            *,
            name: builtins.str = ...,
        ) -> None: ...
        def ClearField(self, field_name: typing_extensions.Literal["name", b"name"]) -> None: ...

    ID_FIELD_NUMBER: builtins.int
    IMPORTS_FIELD_NUMBER: builtins.int
    ROOT_FIELD_NUMBER: builtins.int
    META_FIELD_NUMBER: builtins.int
    @property
    def id(self) -> global___Library.LibIdent: ...
    @property
    def imports(self) -> google.protobuf.internal.containers.RepeatedScalarFieldContainer[builtins.str]: ...
    @property
    def root(self) -> global___Library.NS: ...
    @property
    def meta(self) -> edgir.common_pb2.Metadata: ...
    def __init__(
        self,
        *,
        id: global___Library.LibIdent | None = ...,
        imports: collections.abc.Iterable[builtins.str] | None = ...,
        root: global___Library.NS | None = ...,
        meta: edgir.common_pb2.Metadata | None = ...,
    ) -> None: ...
    def HasField(self, field_name: typing_extensions.Literal["id", b"id", "meta", b"meta", "root", b"root"]) -> builtins.bool: ...
    def ClearField(self, field_name: typing_extensions.Literal["id", b"id", "imports", b"imports", "meta", b"meta", "root", b"root"]) -> None: ...

global___Library = Library

@typing_extensions.final
class Design(google.protobuf.message.Message):
    """* This is a Design for an embedded system at some level of abstraction."""

    DESCRIPTOR: google.protobuf.descriptor.Descriptor

    CONTENTS_FIELD_NUMBER: builtins.int
    @property
    def contents(self) -> edgir.elem_pb2.HierarchyBlock:
        """* Delegate the actual contents of the design to a hierarchy block, for which ports are ignored"""
    def __init__(
        self,
        *,
        contents: edgir.elem_pb2.HierarchyBlock | None = ...,
    ) -> None: ...
    def HasField(self, field_name: typing_extensions.Literal["contents", b"contents"]) -> builtins.bool: ...
    def ClearField(self, field_name: typing_extensions.Literal["contents", b"contents"]) -> None: ...

global___Design = Design
