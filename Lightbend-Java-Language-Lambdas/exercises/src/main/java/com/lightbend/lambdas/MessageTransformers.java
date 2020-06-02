package com.lightbend.lambdas;

import java.time.Instant;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

class MessageTransformers {
    static MessageTransformer<String> toString = new MessageTransformer<String>() {
        @Override
        public String apply(Message message) {
            return message.toString();
        }
    };

    static MessageTransformer<Instant> toTimestamp = new MessageTransformer<Instant>() {
        @Override
        public Instant apply(Message message) {
            return message.getTimestamp();
        }
    };

    static MessageTransformer<Message> toUpperCase = new MessageTransformer<Message>() {
        @Override
        public Message apply(Message message) {
            return new Message(message.getId(), message.getTimestamp(), message.getContent().toUpperCase());
        }
    };

    static <T> List<T> mapMessages(List<Message> messages, MessageTransformer<T> transformer) {
        return messages
                .stream()
                .map(transformFunction(transformer))
                .collect(Collectors.toList());
    }

    private static <T> Function<Message, T> transformFunction(MessageTransformer<T> transformer) {
        return new Function<Message, T>() {
            @Override
            public T apply(Message message) {
                return transformer.apply(message);
            }
        };
    }
}
