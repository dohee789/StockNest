import React from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { IconEgg, IconEggCracked, IconHome, IconChartLine, IconArrowsExchange, IconWallet, IconHistory, IconUser, IconChartCandle, IconChartBar } from '@tabler/icons-react';
import { IconChicken } from '../../components/icons/GrowthIcons';
import classes from './Sidebar.module.css';

const navGroups = [
    {
        key: 'basic',
        label: '기초반',
        GroupIcon: IconEgg,
        items: [
            { path: '/', label: '홈', Icon: IconHome },
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

const Sidebar = () => {
    const navigate = useNavigate();
    const location = useLocation();

    return (
        <aside className={classes.sidebar}>
            <div className={classes.logo} onClick={() => navigate('/')}>
                <IconHome size={20} stroke={1.5} className={classes.logoIcon} />
                <span className={classes.logoText}>StockNest</span>
                <span className={classes.logoTag}>HTS</span>
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
                                    onClick={() => navigate(path)}
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
        </aside>
    );
};

export default Sidebar;
