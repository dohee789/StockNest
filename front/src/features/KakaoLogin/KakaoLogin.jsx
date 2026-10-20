import React, { useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useApp } from '../../context/AppContext';
import toast from 'react-hot-toast';

const BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8090';

const KakaoLogin = () => {
    const { code } = useParams();
    const { login } = useApp();
    const navigate = useNavigate();
    const didRun = useRef(false);

    useEffect(() => {
        if (!code || didRun.current) return;
        didRun.current = true;

        const doLogin = async () => {
            try {
                const res = await fetch(`${BASE_URL}/afterlogin?code=${encodeURIComponent(code)}`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                });
                if (res.ok) {
                    const data = await res.json();
                    login(data.id, data.name);
                    toast.success(`${data.name}님 환영합니다!`);
                    navigate('/');
                } else {
                    toast.error('카카오 로그인에 실패했습니다');
                    navigate('/');
                }
            } catch {
                toast.error('로그인 중 오류가 발생했습니다');
                navigate('/');
            }
        };

        doLogin();
    }, [code, login, navigate]);

    return (
        <div style={{ padding: '60px 20px', textAlign: 'center', color: 'var(--text-muted)' }}>
            카카오 로그인 처리 중...
        </div>
    );
};

export default KakaoLogin;
