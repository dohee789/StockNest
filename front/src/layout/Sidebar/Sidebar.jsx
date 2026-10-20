import React from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { IconEgg, IconEggCracked, IconHome, IconChartLine, IconArrowsExchange, IconWallet, IconHistory, IconUser, IconChartCandle, IconChartBar, IconSchool } from '@tabler/icons-react';
import { IconChicken, IconNest } from '../../components/icons/GrowthIcons';
import classes from './Sidebar.module.css';

const navGroups = [
    {
        key: 'basic',
        label: '기초반',
        GroupIcon: IconEgg,
        items: [
            { path: '/', label: '홈', Icon: IconHome },
            { path: '/tutorial', label: '투자 가이드', Icon: IconSchool },
            { path: '/stocks', label: '시세', Icon: IconChartLine },
            { path: '/order', label: '주문', Icon: IconArrowsExchange },
            { path: '/portfolio', label: '내주식', Icon: IconWallet },
        ],
    },
    {
        key: 'advanced',
        label: '응용반',
        GroupIcon: IconEggCracked,
        items: [
            { path: '/etf', label: 'ETF', Icon: IconChartBar, tooltip: '여러 주식을 한 바구니에 담은 상품' },
            { path: '/futures', label: '선물', Icon: IconChartCandle, tooltip: '레버리지 상품 — 고위험 고수익' },
            { path: '/history', label: '거래내역', Icon: IconHistory },
        ],
    },
    {
        key: 'manage',
        label: '관리반',
        GroupIcon: IconChicken,
        items: [
            { path: '/my', label: 'MY', Icon: IconUser },
        ],
    },
];

const Sidebar = ({ mobileOpen, onClose }) => {
    const navigate = useNavigate();
    const location = useLocation();

    const handleNav = (path) => {
        navigate(path);
        onClose?.();
    };

    const navContent = (
        <>
            <div className={classes.logo} onClick={() => handleNav('/')}>
                <IconNest size={22} stroke={1.5} className={classes.logoIcon} />
                <span className={classes.logoText}>삐약스탁</span>
            </div>

            <nav className={classes.nav}>
                {navGroups.map((group) => (
                    <div key={group.key} className={classes.group}>
                        <div className={classes.groupHeader}>
                            <group.GroupIcon size={16} stroke={1.5} className={classes.groupIcon} />
                            <span className={classes.groupLabel}>{group.label}</span>
                        </div>
                        {group.items.map(({ path, label, Icon, tooltip }) => {
                            const isActive = location.pathname === path
                                || (path !== '/' && location.pathname.startsWith(path));
                            return (
                                <button
                                    key={path}
                                    className={`${classes.navItem} ${isActive ? classes.active : ''}`}
                                    onClick={() => handleNav(path)}
                                    {...(tooltip ? { 'data-tooltip': tooltip } : {})}
                                >
                                    <Icon size={20} stroke={1.5} className={classes.navIcon} />
                                    <span className={classes.navLabel}>{label}</span>
                                </button>
                            );
                        })}
                    </div>
                ))}
            </nav>
        </>
    );

    return (
        <>
            {/* 데스크탑 사이드바 */}
            <aside className={classes.sidebar}>
                {navContent}
            </aside>

            {/* 모바일 드로어 */}
            {mobileOpen && (
                <>
                    <div className={classes.drawerOverlay} onClick={onClose} />
                    <aside className={classes.drawer}>
                        {navContent}
                    </aside>
                </>
            )}
        </>
    );
};

export default Sidebar;
