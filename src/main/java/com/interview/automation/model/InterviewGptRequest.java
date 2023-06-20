package com.interview.automation.model;

import java.util.ArrayList;
import java.util.List;

public class InterviewGptRequest {
    private String model;
    private List<Message> messages;

    public InterviewGptRequest(String model, String prompt) {
        this.model = model;

        this.messages = new ArrayList<>();
        this.messages.add(new Message("user", prompt));
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

	@Override
	public String toString() {
		return "InterviewGptRequest [model=" + model + ", messages=" + messages + "]";
	}
    
    
}