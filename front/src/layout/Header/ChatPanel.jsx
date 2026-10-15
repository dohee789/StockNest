import React, { useState, useRef, useEffect, useCallback } from 'react';
import { useApp } from '../../context/AppContext';
import SmartToyRoundedIcon from '@mui/icons-material/SmartToyRounded';
import DeleteOutlineRoundedIcon from '@mui/icons-material/DeleteOutlineRounded';
import CloseRoundedIcon from '@mui/icons-material/CloseRounded';
import SendRoundedIcon from '@mui/icons-material/SendRounded';
import Markdown from 'markdown-to-jsx';
import classes from './ChatPanel.module.css';

const BASE_URL = 'http://localhost:8090';

const QUICK_QUESTIONS = [
    '내 포트폴리오 분석해줘',
    '분산투자가 뭐야?',
    'ETF 추천해줘',
    '손절매 기준은?',
];

const ChatPanel = ({ onClose }) => {
    const { userId } = useApp();
    const [messages, setMessages] = useState([]);
    const [input, setInput] = useState('');
    const [loading, setLoading] = useState(false);
    const [streamingText, setStreamingText] = useState('');
    const [historyLoaded, setHistoryLoaded] = useState(false);
    const messagesEndRef = useRef(null);
    const inputRef = useRef(null);

    const scrollToBottom = useCallback(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, []);

    // 대화 기록 로드
    useEffect(() => {
        if (!userId || historyLoaded) return;
        const loadHistory = async () => {
            try {
                const res = await fetch(`${BASE_URL}/chatbot/history/${userId}`);
                if (res.ok) {
                    const data = await res.json();
                    setMessages(data);
                }
            } catch { /* ignore */ }
            setHistoryLoaded(true);
        };
        loadHistory();
    }, [userId, historyLoaded]);

    useEffect(() => {
        scrollToBottom();
    }, [messages, loading, streamingText, scrollToBottom]);

    useEffect(() => {
        inputRef.current?.focus();
    }, []);

    // 스트리밍 메시지 전송
    const sendMessage = async (text) => {
        const msg = text || input.trim();
        if (!msg || loading) return;

        const userMsg = { role: 'user', content: msg, createdAt: new Date().toISOString() };
        setMessages(prev => [...prev, userMsg]);
        setInput('');
        setLoading(true);
        setStreamingText('');

        try {
            const url = `${BASE_URL}/chatbot/stream?customerId=${encodeURIComponent(userId)}&message=${encodeURIComponent(msg)}`;
            const res = await fetch(url);

            if (!res.ok) throw new Error('Stream failed');

            const reader = res.body.getReader();
            const decoder = new TextDecoder();
            let accumulated = '';

            while (true) {
                const { done, value } = await reader.read();
                if (done) break;

                const chunk = decoder.decode(value, { stream: true });
                const lines = chunk.split('\n');

                for (const line of lines) {
                    if (line.startsWith('event:done')) {
                        // 스트리밍 완료 → 메시지로 확정
                        setMessages(prev => [...prev, {
                            role: 'assistant',
                            content: accumulated,
                            createdAt: new Date().toISOString(),
                        }]);
                        setStreamingText('');
                        setLoading(false);
                        return;
                    }
                    if (line.startsWith('event:error')) {
                        setMessages(prev => [...prev, {
                            role: 'assistant',
                            content: '오류가 발생했습니다. 다시 시도해주세요.',
                            createdAt: new Date().toISOString(),
                        }]);
                        setStreamingText('');
                        setLoading(false);
                        return;
                    }
                    if (line.startsWith('data:')) {
                        const data = line.substring(5);
                        accumulated += data;
                        setStreamingText(accumulated);
                    }
                }
            }

            // 스트림이 끝났는데 done 이벤트가 없었을 경우
            if (accumulated) {
                setMessages(prev => [...prev, {
                    role: 'assistant',
                    content: accumulated,
                    createdAt: new Date().toISOString(),
                }]);
            }
            setStreamingText('');
        } catch {
            setMessages(prev => [...prev, {
                role: 'assistant',
                content: '서버 연결에 실패했습니다. 잠시 후 다시 시도해주세요.',
                createdAt: new Date().toISOString(),
            }]);
            setStreamingText('');
        } finally {
            setLoading(false);
        }
    };

    const handleClear = async () => {
        try {
            await fetch(`${BASE_URL}/chatbot/history/${userId}`, { method: 'DELETE' });
            setMessages([]);
        } catch { /* ignore */ }
    };

    const handleKeyDown = (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            sendMessage();
        }
    };

    return (
        <>
            <div className={classes.overlay} onClick={onClose} />
            <div className={classes.panel}>
                {/* 헤더 */}
                <div className={classes.panelHeader}>
                    <div className={classes.headerLeft}>
                        <SmartToyRoundedIcon className={classes.headerIcon} />
                        <span className={classes.headerTitle}>AI 투자 어시스턴트</span>
                    </div>
                    <div className={classes.headerActions}>
                        <button className={classes.headerBtn} onClick={handleClear} title="대화 초기화">
                            <DeleteOutlineRoundedIcon style={{ fontSize: 18 }} />
                        </button>
                        <button className={classes.headerBtn} onClick={onClose} title="닫기">
                            <CloseRoundedIcon style={{ fontSize: 18 }} />
                        </button>
                    </div>
                </div>

                {/* 메시지 */}
                <div className={classes.messages}>
                    {messages.length === 0 && !loading && (
                        <div className={classes.welcome}>
                            <SmartToyRoundedIcon className={classes.welcomeIcon} />
                            <p className={classes.welcomeTitle}>안녕하세요! 투자 도우미입니다</p>
                            <p className={classes.welcomeDesc}>
                                포트폴리오 분석, 투자 전략, 주식 용어 등{'\n'}
                                무엇이든 물어보세요!
                            </p>
                            <div className={classes.quickActions}>
                                {QUICK_QUESTIONS.map(q => (
                                    <button
                                        key={q}
                                        className={classes.quickBtn}
                                        onClick={() => sendMessage(q)}
                                    >
                                        {q}
                                    </button>
                                ))}
                            </div>
                        </div>
                    )}

                    {messages.map((msg, i) => (
                        <div
                            key={msg.id || i}
                            className={`${classes.bubble} ${classes[msg.role]}`}
                        >
                            {msg.role === 'assistant' ? (
                                <div className={classes.markdown}>
                                    <Markdown>{msg.content}</Markdown>
                                </div>
                            ) : msg.content}
                        </div>
                    ))}

                    {/* 스트리밍 중 실시간 표시 */}
                    {streamingText && (
                        <div className={`${classes.bubble} ${classes.assistant}`}>
                            <div className={classes.markdown}>
                                <Markdown>{streamingText}</Markdown>
                            </div>
                        </div>
                    )}

                    {loading && !streamingText && (
                        <div className={`${classes.bubble} ${classes.typing}`}>
                            <div className={classes.typingDots}>
                                <span /><span /><span />
                            </div>
                        </div>
                    )}

                    <div ref={messagesEndRef} />
                </div>

                {/* 입력 */}
                <div className={classes.inputArea}>
                    <input
                        ref={inputRef}
                        className={classes.input}
                        value={input}
                        onChange={e => setInput(e.target.value)}
                        onKeyDown={handleKeyDown}
                        placeholder="메시지를 입력하세요..."
                        disabled={loading}
                    />
                    <button
                        className={classes.sendBtn}
                        onClick={() => sendMessage()}
                        disabled={!input.trim() || loading}
                    >
                        <SendRoundedIcon style={{ fontSize: 18 }} />
                    </button>
                </div>
            </div>
        </>
    );
};

export default ChatPanel;
