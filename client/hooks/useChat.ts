'use client';

import { useState } from 'react';
import { ChatMessage, ConversationSummary } from '@/types/chat';
import { apiClient } from '@/lib/api-client';

export function useChat() {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [currentConversationId, setCurrentConversationId] = useState<string | null>(null);

  const sendMessage = async (content: string) => {
    if (!content.trim()) return;

    const userMessage: ChatMessage = {
      id: Date.now().toString(),
      role: 'user',
      content,
      timestamp: Date.now(),
    };

    setMessages((prev) => [...prev, userMessage]);
    setIsLoading(true);
    setError(null);

    try {
      console.log('Sending chat message:', { content, conversationId: currentConversationId });
      console.log('API URL:', apiClient);
      
      const response = await apiClient.post<{ 
        response: string; 
        conversationId: string;
        timestamp: number;
      }>(
        '/chat',
        { 
          message: content,
          conversationId: currentConversationId 
        }
      );
      
      console.log('Chat response received:', response);

      // Update conversation ID if it's a new conversation
      if (response.conversationId && response.conversationId !== currentConversationId) {
        setCurrentConversationId(response.conversationId);
      }

      const assistantMessage: ChatMessage = {
        id: (Date.now() + 1).toString(),
        role: 'assistant',
        content: response.response,
        timestamp: response.timestamp,
      };

      setMessages((prev) => [...prev, assistantMessage]);
    } catch (err) {
      console.error('Chat error:', err);
      const errorMessage = err instanceof Error ? err.message : 'Erreur de communication';
      setError(errorMessage);
      
      const errorResponse: ChatMessage = {
        id: (Date.now() + 1).toString(),
        role: 'assistant',
        content: `Désolé, une erreur s'est produite: ${errorMessage}`,
        timestamp: Date.now(),
      };
      
      setMessages((prev) => [...prev, errorResponse]);
    } finally {
      setIsLoading(false);
    }
  };

  const clearMessages = () => {
    setMessages([]);
    setError(null);
    setCurrentConversationId(null);
  };

  const loadConversation = async (conversationId: string) => {
    setIsLoading(true);
    setError(null);
    
    try {
      const conversation = await apiClient.get<{
        id: string;
        title: string;
        messages: Array<{
          role: 'user' | 'assistant';
          content: string;
          timestamp: number;
        }>;
      }>(`/chat/conversations/${conversationId}`);

      setCurrentConversationId(conversation.id);
      
      const loadedMessages: ChatMessage[] = conversation.messages.map((msg, index) => ({
        id: `${conversationId}-${index}`,
        role: msg.role,
        content: msg.content,
        timestamp: msg.timestamp,
      }));

      setMessages(loadedMessages);
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Erreur lors du chargement';
      setError(errorMessage);
    } finally {
      setIsLoading(false);
    }
  };

  const getConversations = async (activeOnly = true): Promise<ConversationSummary[]> => {
    try {
      const conversations = await apiClient.get<ConversationSummary[]>(
        `/chat/conversations?activeOnly=${activeOnly}`
      );
      return conversations;
    } catch (err) {
      console.error('Error fetching conversations:', err);
      return [];
    }
  };

  return {
    messages,
    isLoading,
    error,
    currentConversationId,
    sendMessage,
    clearMessages,
    loadConversation,
    getConversations,
  };
}
