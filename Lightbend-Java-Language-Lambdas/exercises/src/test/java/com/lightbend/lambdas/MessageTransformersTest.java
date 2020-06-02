package com.lightbend.lambdas;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class MessageTransformersTest {

    @Test
    void toString_shouldReturnTheStringRepresentationOfTheMessage() {
        Message message = new Message("Some Message");

        String actual = MessageTransformers.toString.apply(message);
        String expected = "Message("+message.getId().toString()+","+message.getTimestamp().toString()+","+message.getContent()+")";

        assertEquals(expected, actual);
    }

    @Test
    void toTimestamp_shouldReturnTheTimestampFromTheMessage() {
        Message message = new Message("Some Message");

        Instant timestamp = MessageTransformers.toTimestamp.apply(message);

        assertEquals(message.getTimestamp(), timestamp);
    }

    @Test
    void toUpperCase_shouldUppercaseTheMessageContent() {
        Message message = new Message("Some Message");

        Message upperCase = MessageTransformers.toUpperCase.apply(message);

        assertEquals(message.getId(), upperCase.getId());
        assertEquals(message.getTimestamp(), upperCase.getTimestamp());
        assertEquals(message.getContent().toUpperCase(), upperCase.getContent());
    }

    @Test
    void map_shouldMapAListOfMessagesToAListOfTheRequestedType() {
        List<Message> messages = new ArrayList<>();
        messages.add(new Message("Message 1"));
        messages.add(new Message("Message 2"));
        messages.add(new Message("Message 3"));
        messages.add(new Message("Message 4"));

        List<String> messageStrings = MessageTransformers.mapMessages(messages, MessageTransformers.toString);

        assertEquals(messages.get(0).toString(), messageStrings.get(0));
        assertEquals(messages.get(1).toString(), messageStrings.get(1));
        assertEquals(messages.get(2).toString(), messageStrings.get(2));
        assertEquals(messages.get(3).toString(), messageStrings.get(3));
    }

}
