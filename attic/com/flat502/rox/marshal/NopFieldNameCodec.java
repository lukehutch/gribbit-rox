package com.flat502.rox.marshal;

// TODO: Document
public class NopFieldNameCodec implements FieldNameCodec {
	@Override
    public String encodeFieldName(String name) {
		return name;
	}
	
	@Override
    public String decodeFieldName(String name) {
		return name;
	}
}
