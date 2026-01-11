'use client';

import React, { useState, useRef, useEffect } from 'react';
import { X, Send, MessageCircle, Minimize2, Sparkles, Trash2 } from 'lucide-react';
import { useChat } from '@/hooks/useChat';
import { ChatMessage } from './ChatMessage';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { LoadingSpinner } from '@/components/ui/LoadingSpinner';

export function ChatWidget() {
  const [isOpen, setIsOpen] = useState(false);
  const [inputValue, setInputValue] = useState('');
  const [isAnimating, setIsAnimating] = useState(false);
  const { messages, isLoading, sendMessage, clearMessages } = useChat();
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  useEffect(() => {
    if (isOpen) {
      setIsAnimating(true);
    }
  }, [isOpen]);

  const handleSend = async () => {
    if (!inputValue.trim() || isLoading) return;
    
    await sendMessage(inputValue);
    setInputValue('');
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleClose = () => {
    setIsAnimating(false);
    setTimeout(() => setIsOpen(false), 200);
  };

  const suggestedQuestions = [
    { icon: '📊', text: "Résumé de mes devices", query: "Donne-moi un résumé complet de l'état de mes devices" },
    { icon: '⚠️', text: "Devices avec anomalies", query: "Quels sont les devices qui présentent des anomalies ou des problèmes ?" },
    { icon: '🌡️', text: "Températures des machines", query: "Quel est l'état de la température de tous mes devices ?" },
    { icon: '💻', text: "Utilisation CPU/Mémoire", query: "Montre-moi l'utilisation CPU et mémoire de mes devices" },
  ];

  return (
    <>
      {/* Floating Chat Button */}
      {!isOpen && (
        <button
          type="button"
          onClick={() => setIsOpen(true)}
          className="fixed bottom-6 right-6 bg-gradient-to-r from-blue-600 to-indigo-600 text-white rounded-full p-4 shadow-2xl hover:shadow-blue-500/50 hover:scale-110 transition-all duration-300 z-50 group animate-pulse-slow"
          aria-label="Ouvrir le chat"
        >
          <MessageCircle size={28} className="group-hover:rotate-12 transition-transform" />
          <span className="absolute -top-1 -right-1 h-4 w-4 bg-green-400 rounded-full border-2 border-white animate-ping"></span>
          <span className="absolute -top-1 -right-1 h-4 w-4 bg-green-400 rounded-full border-2 border-white"></span>
        </button>
      )}

      {/* Chat Window */}
      {isOpen && (
        <div className={`fixed bottom-6 right-6 w-[420px] h-[650px] bg-gradient-to-br from-white to-gray-50 rounded-2xl shadow-2xl flex flex-col z-50 border border-gray-200 transition-all duration-300 ${
          isAnimating ? 'scale-100 opacity-100' : 'scale-95 opacity-0'
        }`}>
          {/* Header */}
          <div className="bg-gradient-to-r from-blue-600 via-indigo-600 to-purple-600 text-white px-5 py-4 rounded-t-2xl flex items-center justify-between shadow-lg">
            <div className="flex items-center gap-3">
              <div className="relative">
                <Sparkles size={24} className="animate-pulse" />
                <span className="absolute -bottom-1 -right-1 h-3 w-3 bg-green-400 rounded-full border-2 border-white"></span>
              </div>
              <div>
                <h3 className="font-bold text-lg">Assistant IA</h3>
                <p className="text-xs text-blue-100">Propulsé par Gemini</p>
              </div>
            </div>
            <div className="flex gap-1">
              {messages.length > 0 && (
                <button
                  type="button"
                  onClick={clearMessages}
                  className="hover:bg-white/20 rounded-lg p-2 transition-all duration-200 group"
                  aria-label="Effacer"
                  title="Effacer la conversation"
                >
                  <Trash2 size={18} className="group-hover:scale-110 transition-transform" />
                </button>
              )}
              <button
                type="button"
                onClick={handleClose}
                className="hover:bg-white/20 rounded-lg p-2 transition-all duration-200 group"
                aria-label="Minimiser"
              >
                <Minimize2 size={18} className="group-hover:scale-110 transition-transform" />
              </button>
              <button
                type="button"
                onClick={handleClose}
                className="hover:bg-white/20 rounded-lg p-2 transition-all duration-200 group"
                aria-label="Fermer"
              >
                <X size={18} className="group-hover:rotate-90 group-hover:scale-110 transition-all" />
              </button>
            </div>
          </div>

          {/* Messages Area */}
          <div className="flex-1 overflow-y-auto p-5 bg-gradient-to-b from-gray-50 to-white custom-scrollbar">
            {messages.length === 0 ? (
              <div className="flex flex-col items-center justify-center h-full text-gray-600 animate-fade-in">
                <div className="relative mb-6">
                  <div className="absolute inset-0 bg-gradient-to-r from-blue-400 to-purple-400 rounded-full blur-2xl opacity-20 animate-pulse"></div>
                  <Sparkles size={64} className="relative text-indigo-600 animate-bounce-slow" />
                </div>
                <h4 className="text-xl font-bold text-gray-800 mb-2">Bonjour ! 👋</h4>
                <p className="text-center text-gray-600 mb-6 max-w-xs">
                  Je suis votre assistant IA intelligent.
                  <br />
                  Posez-moi des questions sur vos devices !
                </p>
                <div className="grid grid-cols-1 gap-3 w-full px-2">
                  {suggestedQuestions.map((suggestion, index) => (
                    <button
                      type="button"
                      key={index}
                      onClick={() => sendMessage(suggestion.query)}
                      disabled={isLoading}
                      className="group relative overflow-hidden text-left px-4 py-3 text-sm bg-white border-2 border-gray-200 rounded-xl hover:border-indigo-400 hover:shadow-lg transition-all duration-300 transform hover:-translate-y-1"
                    >
                      <div className="absolute inset-0 bg-gradient-to-r from-blue-50 to-indigo-50 opacity-0 group-hover:opacity-100 transition-opacity"></div>
                      <div className="relative flex items-center gap-3">
                        <span className="text-2xl">{suggestion.icon}</span>
                        <span className="font-medium text-gray-700 group-hover:text-indigo-600 transition-colors">
                          {suggestion.text}
                        </span>
                      </div>
                    </button>
                  ))}
                </div>
              </div>
            ) : (
              <>
                {messages.map((message, index) => (
                  <div
                    key={message.id}
                    className="animate-slide-up"
                    style={{ animationDelay: `${index * 50}ms` }}
                  >
                    <ChatMessage message={message} />
                  </div>
                ))}
                {isLoading && (
                  <div className="flex justify-start mb-4 animate-fade-in">
                    <div className="bg-gradient-to-r from-gray-100 to-gray-50 rounded-2xl px-5 py-3 border border-gray-200 shadow-sm">
                      <div className="flex items-center gap-2">
                        <LoadingSpinner size="sm" />
                        <span className="text-sm text-gray-600">L'assistant réfléchit...</span>
                      </div>
                    </div>
                  </div>
                )}
                <div ref={messagesEndRef} />
              </>
            )}
          </div>

          {/* Input Area */}
          <div className="p-4 border-t border-gray-200 bg-white rounded-b-2xl">
            <div className="flex gap-2 items-end">
              <div className="flex-1 relative">
                <Input
                  value={inputValue}
                  onChange={(e) => setInputValue(e.target.value)}
                  onKeyPress={handleKeyPress}
                  placeholder="Posez votre question..."
                  disabled={isLoading}
                  className="w-full pr-10 py-3 rounded-xl border-2 border-gray-200 focus:border-indigo-400 focus:ring-2 focus:ring-indigo-200 transition-all"
                />
                {inputValue && (
                  <button
                    type="button"
                    onClick={() => setInputValue('')}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 transition-colors"
                  >
                    <X size={16} />
                  </button>
                )}
              </div>
              <Button
                onClick={handleSend}
                disabled={isLoading || !inputValue.trim()}
                className={`px-4 py-3 rounded-xl transition-all duration-300 ${
                  inputValue.trim() && !isLoading
                    ? 'bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 shadow-lg hover:shadow-xl transform hover:scale-105'
                    : 'bg-gray-300 cursor-not-allowed'
                }`}
              >
                {isLoading ? (
                  <LoadingSpinner size="sm" />
                ) : (
                  <Send size={20} className={inputValue.trim() ? 'animate-pulse-slow' : ''} />
                )}
              </Button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
