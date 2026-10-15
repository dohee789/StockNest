import React, { useState } from 'react';
import toast from 'react-hot-toast';
import SearchRoundedIcon from '@mui/icons-material/SearchRounded';
import AddRoundedIcon from '@mui/icons-material/AddRounded';
import { useApp } from '../../context/AppContext';
import { API_BASE_URL } from '../../config/api';
import classes from './StockSearchBar.module.css';

const BASE_URL = API_BASE_URL;

const StockSearchBar = ({ onStockAdded }) => {
    const { userId, isLoggedIn } = useApp();
    const [query, setQuery] = useState('');
    const [results, setResults] = useState([]);
    const [, setSearching] = useState(false);

    const handleSearch = async (e) => {
        const val = e.target.value;
        setQuery(val);
        if (!val.trim() || val.length < 1) {
            setResults([]);
            return;
        }
        setSearching(true);
        try {
            const res = await fetch(`${BASE_URL}/stocks`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ name: val }),
            });
            if (res.ok) {
                const data = await res.json();
                setResults((data || []).slice(0, 8));
            }
        } catch {
            setResults([]);
        } finally {
            setSearching(false);
        }
    };

    const handleAdd = async (stock) => {
        if (!isLoggedIn) {
            toast.error('로그인이 필요합니다');
            return;
        }
        try {
            const res = await fetch(`${BASE_URL}/stocks/watch`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ code: stock.code, name: stock.name, customerId: userId }),
            });
            if (res.ok) {
                toast.success(`${stock.name} 관심종목 추가`);
                setQuery('');
                setResults([]);
                onStockAdded?.();
            } else {
                const msg = await res.text();
                toast.error(msg || '이미 등록된 종목입니다');
            }
        } catch {
            toast.error('추가 중 오류가 발생했습니다');
        }
    };

    return (
        <div className={classes.wrap}>
            <div className={classes.searchBox}>
                <SearchRoundedIcon className={classes.searchIcon} />
                <input
                    className={classes.input}
                    placeholder="종목명 검색 후 관심종목 추가"
                    value={query}
                    onChange={handleSearch}
                />
            </div>
            {results.length > 0 && (
                <div className={classes.dropdown}>
                    {results.map(stock => (
                        <div key={stock.code} className={classes.dropdownItem}>
                            <div className={classes.dropdownInfo}>
                                <span className={classes.dropdownName}>{stock.name}</span>
                                <span className={classes.dropdownCode}>{stock.code}</span>
                            </div>
                            <button className={classes.addBtn} onClick={() => handleAdd(stock)}>
                                <AddRoundedIcon fontSize="small" />
                            </button>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};

export default StockSearchBar;
