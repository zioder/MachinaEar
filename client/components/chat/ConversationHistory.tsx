'use client';

import React, { useEffect, useState } from 'react';
import { MessageSquare, Clock, Archive, Loader2 } from 'lucide-react';
import { ConversationSummary } from '@/types/chat';

interface ConversationHistoryProps {
  onSelectConversation: (conversationId: string) => void;
  onNewConversation: () => void;
  getConversations: (activeOnly: boolean) => Promise<ConversationSummary[]>;
  currentConversationId: string | null;
}

export function ConversationHistory({
  onSelectConversation,
  onNewConversation,
  getConversations,
  currentConversationId,
}: ConversationHistoryProps) {
  const [conversations, setConversations] = useState<ConversationSummary[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [showArchived, setShowArchived] = useState(false);

  useEffect(() => {
    loadConversations();
  }, [showArchived]);

  const loadConversations = async () => {
    setIsLoading(true);
    try {
      const data = await getConversations(!showArchived);
      setConversations(data);
    } catch (error) {
      console.error('Error loading conversations:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const formatDate = (timestamp: number) => {
    const date = new Date(timestamp);
    const now = new Date();
    const diffInHours = (now.getTime() - date.getTime()) / (1000 * 60 * 60);

    if (diffInHours < 24) {
      return date.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
    } else if (diffInHours < 48) {
      return 'Hier';
    } else if (diffInHours < 168) {
      return date.toLocaleDateString('fr-FR', { weekday: 'short' });
    } else {
      return date.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' });
    }
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-8">
        <Loader2 className="animate-spin text-blue-600" size={24} />
      </div>
    );
  }

  return (
    <div className="flex flex-col h-full">
      <div className="flex items-center justify-between mb-4 px-2">
        <h3 className="text-sm font-semibold text-gray-700">Conversations</h3>
        <button
          onClick={() => setShowArchived(!showArchived)}
          className="text-xs text-blue-600 hover:text-blue-700 flex items-center gap-1"
        >
          <Archive size={14} />
          {showArchived ? 'Actives' : 'Archivées'}
        </button>
      </div>

      <button
        onClick={onNewConversation}
        className="mb-3 px-4 py-2 bg-gradient-to-r from-blue-600 to-indigo-600 text-white rounded-lg hover:shadow-lg transition-all duration-200 text-sm font-medium"
      >
        + Nouvelle conversation
      </button>

      <div className="flex-1 overflow-y-auto space-y-2">
        {conversations.length === 0 ? (
          <div className="text-center py-8 text-gray-400 text-sm">
            <MessageSquare className="mx-auto mb-2 opacity-30" size={32} />
            <p>Aucune conversation</p>
          </div>
        ) : (
          conversations.map((conv) => (
            <button
              key={conv.id}
              onClick={() => onSelectConversation(conv.id)}
              className={`w-full text-left px-3 py-3 rounded-lg transition-all duration-200 ${
                currentConversationId === conv.id
                  ? 'bg-blue-100 border-2 border-blue-500'
                  : 'bg-white hover:bg-gray-50 border border-gray-200'
              }`}
            >
              <div className="flex items-start justify-between gap-2">
                <div className="flex-1 min-w-0">
                  <h4 className="font-medium text-sm text-gray-900 truncate">
                    {conv.title}
                  </h4>
                  <div className="flex items-center gap-2 mt-1">
                    <span className="text-xs text-gray-500 flex items-center gap-1">
                      <Clock size={12} />
                      {formatDate(conv.lastActivityAt)}
                    </span>
                    <span className="text-xs text-gray-400">
                      • {conv.messageCount} msg
                    </span>
                  </div>
                </div>
                {!conv.active && (
                  <Archive size={14} className="text-gray-400 flex-shrink-0" />
                )}
              </div>
            </button>
          ))
        )}
      </div>
    </div>
  );
}
