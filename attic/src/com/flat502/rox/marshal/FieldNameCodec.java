package com.flat502.rox.marshal;

// TODO: Document
public interface FieldNameCodec extends FieldNameEncoder, FieldNameDecoder {
	@Override
    String encodeFieldName(String name);
	@Override
    String decodeFieldName(String name);
}
