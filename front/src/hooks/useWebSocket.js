import { useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useApp } from '../context/AppContext';

const WS_URL = 'http://localhost:8090/ws';

export const useWebSocket = () => {
    const { updateRealtimePrice } = useApp();
    const clientRef = useRef(null);

    useEffect(() => {
        const client = new Client({
            webSocketFactory: () => new SockJS(WS_URL),
            reconnectDelay: 5000,
            onConnect: () => {
                client.subscribe('/topic/stock-prices', (message) => {
                    try {
                        const prices = JSON.parse(message.body);
                        updateRealtimePrice(prices);
                    } catch (e) {
                        console.error('WebSocket message parse error:', e);
                    }
                });
            },
            onStompError: (frame) => {
                console.warn('WebSocket STOMP error:', frame);
            },
        });

        client.activate();
        clientRef.current = client;

        return () => {
            if (clientRef.current?.active) {
                clientRef.current.deactivate();
            }
        };
    }, [updateRealtimePrice]);

    return clientRef;
};
