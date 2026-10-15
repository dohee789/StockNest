import React, { useEffect, useState } from 'react';
import TrendingUpIcon from '@mui/icons-material/TrendingUp';
import TrendingDownIcon from '@mui/icons-material/TrendingDown';
import classes from './MarketIndex.module.css';

const BASE_URL = 'http://localhost:8090/market-index';

const MarketIndex = () => {
    const [indexes, setIndexes] = useState([]);

    useEffect(() => {
        const fetchIndexes = async () => {
            try {
                const res = await fetch(BASE_URL);
                if (res.ok) {
                    const data = await res.json();
                    setIndexes(data);
                }
            } catch (e) {
                // silent fail
            }
        };
        fetchIndexes();
        const interval = setInterval(fetchIndexes, 60000); // 1분마다 갱신
        return () => clearInterval(interval);
    }, []);

    if (indexes.length === 0) return null;

    return (
        <div className={classes.indexBar}>
            {indexes.map((idx) => (
                <div key={idx.name} className={classes.indexItem}>
                    <span className={classes.indexName}>{idx.name}</span>
                    <span className={classes.indexValue}>{idx.value}</span>
                    <div className={`${classes.indexChange} ${idx.up ? classes.rise : classes.fall}`}>
                        {idx.up ? <TrendingUpIcon fontSize="inherit" /> : <TrendingDownIcon fontSize="inherit" />}
                        <span>{idx.changeRate}</span>
                    </div>
                </div>
            ))}
        </div>
    );
};

export default MarketIndex;
