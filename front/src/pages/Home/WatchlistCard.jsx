import React from 'react';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import CloseRoundedIcon from '@mui/icons-material/CloseRounded';
import TrendingUpRoundedIcon from '@mui/icons-material/TrendingUpRounded';
import TrendingDownRoundedIcon from '@mui/icons-material/TrendingDownRounded';
import TrendingFlatRoundedIcon from '@mui/icons-material/TrendingFlatRounded';
import ChevronRightRoundedIcon from '@mui/icons-material/ChevronRightRounded';
import { API_BASE_URL } from '../../config/api';
import classes from './WatchlistCard.module.css';

const BASE_URL = `${API_BASE_URL}/stocks/watch`;

const WatchlistCard = ({ stock, onRemove }) => {
    const navigate = useNavigate();
    const rate = stock.changeRate || '';
    const isRise = rate.startsWith('+') || rate.startsWith('▲');
    const isFall = rate.startsWith('-') || rate.startsWith('▼');

    const handleRemove = async (e) => {
        e.stopPropagation();
        try {
            await fetch(BASE_URL, {
                method: 'DELETE',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ code: stock.code }),
            });
            toast.success(`${stock.name} 관심종목 해제`);
            onRemove();
        } catch {
            toast.error('삭제 중 오류가 발생했습니다');
        }
    };

    const handleClick = () => {
        navigate(`/stock/${stock.code}`);
    };

    return (
        <div className={classes.card} onClick={handleClick}>
            <div className={classes.left}>
                <div className={classes.stockInfo}>
                    <span className={classes.name}>{stock.name}</span>
                    <span className={classes.code}>{stock.code}</span>
                </div>
            </div>
            <div className={classes.right}>
                <div className={`${classes.priceInfo} ${isRise ? classes.rise : isFall ? classes.fall : classes.flat}`}>
                    <span className={classes.price}>{stock.price || '-'}</span>
                    <div className={classes.change}>
                        {isRise ? <TrendingUpRoundedIcon fontSize="small" />
                            : isFall ? <TrendingDownRoundedIcon fontSize="small" />
                                : <TrendingFlatRoundedIcon fontSize="small" />}
                        <span>{stock.changeRate || '-'}</span>
                    </div>
                    <span className={classes.diff}>{stock.change || ''}</span>
                </div>
                <div className={classes.actions}>
                    <ChevronRightRoundedIcon className={classes.chevron} fontSize="small" />
                    <button className={classes.removeBtn} onClick={handleRemove}>
                        <CloseRoundedIcon fontSize="small" />
                    </button>
                </div>
            </div>
        </div>
    );
};

export default WatchlistCard;
