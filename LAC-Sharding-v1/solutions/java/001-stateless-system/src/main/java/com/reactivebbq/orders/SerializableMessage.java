package com.reactivebbq.orders;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use= JsonTypeInfo.Id.CLASS, property = "type")
public interface SerializableMessage {}
