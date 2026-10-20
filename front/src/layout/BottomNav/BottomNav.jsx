import React, { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import {
    IconHome, IconChartLine, IconArrowsExchange, IconWallet,
    IconDotsVertical, IconBuildingBank, IconTimeline, IconHistory,
    IconUser, IconX, IconSchool
} from '@tabler/icons-react';
import classes from './BottomNav.module.css';

const navItems = [
    { path: '/', label: '홈', Icon: IconHome },
    { path: '/stocks', label: '시세', Icon: IconChartLine },
    { path: '/order', label: '주문', Icon: IconArrowsExchange },
    { path: '/portfolio', label: '내주식', Icon: IconWallet },
];

const moreItems = [
    { path: '/tutorial', label: '투자 가이드', Icon: IconSchool },
    { path: '/etf', label: 'ETF', Icon: IconBuildingBank },
    { path: '/futures', label: '선물', Icon: IconTimeline },
    { path: '/history', label: '거래내역', Icon: IconHistory },
    { path: '/my', label: 'MY', Icon: IconUser },
];

const BottomNav = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const [moreOpen, setMoreOpen] = useState(false);

    const isMoreActive = moreItems.some(
        ({ path }) => location.pathname === path || location.pathname.startsWith(path + '/')
    );

    return (
        <>
            {moreOpen && <div className={classes.overlay} onClick={() => setMoreOpen(false)} />}
            {moreOpen && (
                <div className={classes.morePanel}>
                    {moreItems.map(({ path, label, Icon }) => {
                        const isActive = location.pathname === path
                            || location.pathname.startsWith(path + '/');
                        return (
                            <button
                                key={path}
                                className={`${classes.moreItem} ${isActive ? classes.active : ''}`}
                                onClick={() => { navigate(path); setMoreOpen(false); }}
                            >
                                <Icon size={20} stroke={1.5} />
                                <span>{label}</span>
                            </button>
                        );
                    })}
                </div>
            )}
            <nav className={classes.bottomNav}>
                {navItems.map(({ path, label, Icon }) => {
                    const isActive = location.pathname === path
                        || (path !== '/' && location.pathname.startsWith(path));
                    return (
                        <button
                            key={path}
                            className={`${classes.navItem} ${isActive ? classes.active : ''}`}
                            onClick={() => { navigate(path); setMoreOpen(false); }}
                        >
                            <Icon size={22} stroke={1.5} className={classes.navIcon} />
                            <span className={classes.navLabel}>{label}</span>
                        </button>
                    );
                })}
                <button
                    className={`${classes.navItem} ${isMoreActive ? classes.active : ''}`}
                    onClick={() => setMoreOpen(prev => !prev)}
                >
                    {moreOpen
                        ? <IconX size={22} stroke={1.5} className={classes.navIcon} />
                        : <IconDotsVertical size={22} stroke={1.5} className={classes.navIcon} />
                    }
                    <span className={classes.navLabel}>더보기</span>
                </button>
            </nav>
        </>
    );
};

export default BottomNav;
