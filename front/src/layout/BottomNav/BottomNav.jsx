import React from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { IconHome, IconChartLine, IconArrowsExchange, IconWallet, IconUser } from '@tabler/icons-react';
import classes from './BottomNav.module.css';

/* 모바일 하단 네비: 기초반 4개 + 관리반 MY
   응용반(ETF, 선물, 거래내역)은 데스크탑 사이드바 및 홈에서 접근 */
const navItems = [
    { path: '/', label: '홈', Icon: IconHome },
    { path: '/stocks', label: '시세', Icon: IconChartLine },
    { path: '/order', label: '주문', Icon: IconArrowsExchange },
    { path: '/portfolio', label: '내주식', Icon: IconWallet },
    { path: '/my', label: 'MY', Icon: IconUser },
];

const BottomNav = () => {
    const navigate = useNavigate();
    const location = useLocation();

    return (
        <nav className={classes.bottomNav}>
            {navItems.map(({ path, label, Icon }) => {
                const isActive = location.pathname === path
                    || (path !== '/' && location.pathname.startsWith(path));
                return (
                    <button
                        key={path}
                        className={`${classes.navItem} ${isActive ? classes.active : ''}`}
                        onClick={() => navigate(path)}
                    >
                        <Icon size={22} stroke={1.5} className={classes.navIcon} />
                        <span className={classes.navLabel}>{label}</span>
                    </button>
                );
            })}
        </nav>
    );
};

export default BottomNav;
