export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: number;
}

export interface ChatResponse {
  response: string;
  conversationId: string;
  timestamp: number;
}

export interface ConversationSummary {
  id: string;
  title: string;
  lastActivityAt: number;
  messageCount: number;
  active: boolean;
}

export interface ConversationDetail {
  id: string;
  title: string;
  lastActivityAt: number;
  active: boolean;
  messages: {
    role: string;
    content: string;
    timestamp: number;
  }[];
}
