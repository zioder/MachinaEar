'use client';

import React from 'react';
import { ChatMessage as ChatMessageType } from '@/types/chat';
import { User, Sparkles } from 'lucide-react';

interface ChatMessageProps {
  message: ChatMessageType;
}

export function ChatMessage({ message }: ChatMessageProps) {
  const isUser = message.role === 'user';

  return (
    <div className={`flex ${isUser ? 'justify-end' : 'justify-start'} mb-4 group`}>
      <div className={`flex gap-3 max-w-[85%] ${isUser ? 'flex-row-reverse' : 'flex-row'}`}>
        {/* Avatar */}
        <div className={`flex-shrink-0 w-8 h-8 rounded-full flex items-center justify-center ${
          isUser 
            ? 'bg-gradient-to-br from-blue-600 to-indigo-600 shadow-lg' 
            : 'bg-gradient-to-br from-purple-500 to-pink-500 shadow-lg'
        } group-hover:scale-110 transition-transform`}>
          {isUser ? (
            <User size={18} className="text-white" />
          ) : (
            <Sparkles size={18} className="text-white" />
          )}
        </div>

        {/* Message bubble */}
        <div
          className={`relative rounded-2xl px-4 py-3 shadow-sm transition-all duration-300 group-hover:shadow-md ${
            isUser
              ? 'bg-gradient-to-br from-blue-600 to-indigo-600 text-white'
              : 'bg-white text-gray-900 border-2 border-gray-200'
          }`}
        >
          {/* Triangle pointer */}
          <div className={`absolute top-3 w-0 h-0 ${
            isUser
              ? 'right-[-8px] border-l-[8px] border-l-indigo-600 border-t-[8px] border-t-transparent border-b-[8px] border-b-transparent'
              : 'left-[-8px] border-r-[8px] border-r-white border-t-[8px] border-t-transparent border-b-[8px] border-b-transparent'
          }`}></div>

          <div className={`text-sm whitespace-pre-wrap break-words leading-relaxed ${
            isUser ? 'text-white' : 'text-gray-800'
          }`}>
            {message.content}
          </div>
          <div
            className={`text-xs mt-2 flex items-center gap-1 ${
              isUser ? 'text-blue-100' : 'text-gray-500'
            }`}
          >
            <span>
              {new Date(message.timestamp).toLocaleTimeString('fr-FR', {
                hour: '2-digit',
                minute: '2-digit',
              })}
            </span>
          </div>
        </div>
      </div>
    </div>
  );
}
