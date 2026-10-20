import React, { createContext, useContext, useState, useCallback, useEffect } from 'react';

const AppContext = createContext(null);

const BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8090';

export const AppProvider = ({ children }) => {
    const [userId, setUserId] = useState(sessionStorage.getItem('USER') || '');
    const [userName, setUserName] = useState(sessionStorage.getItem('USER_NAME') || '');
    const [realtimePrices, setRealtimePrices] = useState({}); // { stockCode: StockDTO }
    const [notifications, setNotifications] = useState([]);
    const [unreadCount, setUnreadCount] = useState(0);

    const fetchNotifications = useCallback(async (id) => {
        if (!id) return;
        try {
            const [notiRes, countRes] = await Promise.all([
                fetch(`${BASE_URL}/notifications/${id}`),
                fetch(`${BASE_URL}/notifications/${id}/unread-count`),
            ]);
            if (notiRes.ok) setNotifications(await notiRes.json());
            if (countRes.ok) {
                const data = await countRes.json();
                setUnreadCount(data.count);
            }
        } catch { /* ignore */ }
    }, []);

    useEffect(() => {
        if (userId) {
            fetchNotifications(userId);
            const interval = setInterval(() => fetchNotifications(userId), 30000);
            return () => clearInterval(interval);
        } else {
            setNotifications([]);
            setUnreadCount(0);
        }
    }, [userId, fetchNotifications]);

    const markAllRead = useCallback(async () => {
        if (!userId) return;
        try {
            await fetch(`${BASE_URL}/notifications/${userId}/mark-read`, { method: 'POST' });
            setUnreadCount(0);
            setNotifications(prev => prev.map(n => ({ ...n, read: true })));
        } catch { /* ignore */ }
    }, [userId]);

    const deleteNotification = useCallback(async (notificationId) => {
        if (!userId) return;
        try {
            await fetch(`${BASE_URL}/notifications/${userId}/${notificationId}`, { method: 'DELETE' });
            setNotifications(prev => {
                const target = prev.find(n => n.id === notificationId);
                if (target && !target.read) setUnreadCount(c => Math.max(0, c - 1));
                return prev.filter(n => n.id !== notificationId);
            });
        } catch { /* ignore */ }
    }, [userId]);

    const deleteAllNotifications = useCallback(async () => {
        if (!userId) return;
        try {
            await fetch(`${BASE_URL}/notifications/${userId}`, { method: 'DELETE' });
            setNotifications([]);
            setUnreadCount(0);
        } catch { /* ignore */ }
    }, [userId]);

    const login = useCallback((id, name) => {
        sessionStorage.setItem('USER', id);
        sessionStorage.setItem('USER_NAME', name);
        setUserId(id);
        setUserName(name);
    }, []);

    const logout = useCallback(() => {
        sessionStorage.removeItem('USER');
        sessionStorage.removeItem('USER_NAME');
        setUserId('');
        setUserName('');
        setNotifications([]);
        setUnreadCount(0);
    }, []);

    const updateRealtimePrice = useCallback((stockList) => {
        setRealtimePrices(prev => {
            const updated = { ...prev };
            stockList.forEach(stock => {
                updated[stock.code] = stock;
            });
            return updated;
        });
    }, []);

    const isLoggedIn = !!userId;

    return (
        <AppContext.Provider value={{
            userId,
            userName,
            isLoggedIn,
            realtimePrices,
            notifications,
            unreadCount,
            login,
            logout,
            updateRealtimePrice,
            fetchNotifications,
            markAllRead,
            deleteNotification,
            deleteAllNotifications,
        }}>
            {children}
        </AppContext.Provider>
    );
};

export const useApp = () => {
    const ctx = useContext(AppContext);
    if (!ctx) throw new Error('useApp must be used within AppProvider');
    return ctx;
};
