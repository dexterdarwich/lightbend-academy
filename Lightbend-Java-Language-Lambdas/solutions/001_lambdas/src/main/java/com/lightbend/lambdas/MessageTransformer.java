package com.lightbend.lambdas;

@FunctionalInterface
interface MessageTransformer<T> {
    T apply(Message message);
}
