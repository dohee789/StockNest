import React, { useState } from 'react';
import { useApp } from '../../context/AppContext';
import toast from 'react-hot-toast';
import { KAKAO_AUTH_URL } from '../../features/KakaoLogin/OAuth';
import { IconEgg, IconEggCracked, IconChevronRight } from '@tabler/icons-react';
import { IconChick, IconChicken } from '../../components/icons/GrowthIcons';
import classes from './LoginPrompt.module.css';

const BASE_URL = 'http://localhost:8090';

const LoginPrompt = () => {
    const { login } = useApp();
    const [tab, setTab] = useState('login');
    const [form, setForm] = useState({ name: '', email: '', password: '' });
    const [loading, setLoading] = useState(false);

    const handleChange = (e) => setForm(prev => ({ ...prev, [e.target.name]: e.target.value }));

    const handleLogin = async (e) => {
        e.preventDefault();
        setLoading(true);
        try {
            const res = await fetch(`${BASE_URL}/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email: form.email, password: form.password }),
            });
            if (res.ok) {
                const data = await res.json();
                login(data.id, data.name);
                toast.success(`${data.name}님 환영합니다!`);
            } else {
                toast.error('이메일 또는 비밀번호가 틀렸습니다');
            }
        } catch {
            toast.error('로그인 중 오류가 발생했습니다');
        } finally {
            setLoading(false);
        }
    };

    const handleSignup = async (e) => {
        e.preventDefault();
        setLoading(true);
        try {
            const res = await fetch(`${BASE_URL}/join`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ name: form.name, email: form.email, password: form.password }),
            });
            if (res.ok) {
                toast.success('회원가입 완료! 로그인해주세요');
                setTab('login');
                setForm(prev => ({ ...prev, name: '' }));
            } else {
                toast.error('이미 사용 중인 이메일입니다');
            }
        } catch {
            toast.error('회원가입 중 오류가 발생했습니다');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className={classes.container}>
            <div className={classes.hero}>
                <div className={classes.heroIconWrap}>
                    <IconEgg size={48} stroke={1.5} className={classes.heroIcon} />
                </div>
                <h1 className={classes.heroTitle}>삐약스탁</h1>
                <p className={classes.heroDesc}>
                    주식, 일단 까보자! 🐣
                </p>
                <div className={classes.growthPath}>
                    <span className={classes.growthStep}><IconEgg size={22} stroke={1.5} /></span>
                    <IconChevronRight size={14} stroke={2} className={classes.growthArrow} />
                    <span className={classes.growthStep}><IconEggCracked size={22} stroke={1.5} /></span>
                    <IconChevronRight size={14} stroke={2} className={classes.growthArrow} />
                    <span className={classes.growthStep}><IconChick size={22} stroke={1.5} /></span>
                    <IconChevronRight size={14} stroke={2} className={classes.growthArrow} />
                    <span className={classes.growthStep}><IconChicken size={22} stroke={1.5} /></span>
                </div>
            </div>
            <div className={classes.card}>
                <div className={classes.tabs}>
                    <button
                        className={`${classes.tab} ${tab === 'login' ? classes.activeTab : ''}`}
                        onClick={() => setTab('login')}
                    >
                        로그인
                    </button>
                    <button
                        className={`${classes.tab} ${tab === 'signup' ? classes.activeTab : ''}`}
                        onClick={() => setTab('signup')}
                    >
                        회원가입
                    </button>
                </div>
                {tab === 'login' ? (
                    <form className={classes.form} onSubmit={handleLogin}>
                        <input className={classes.input} type="email" name="email" placeholder="이메일" value={form.email} onChange={handleChange} required />
                        <input className={classes.input} type="password" name="password" placeholder="비밀번호" value={form.password} onChange={handleChange} required />
                        <button className={classes.submitBtn} type="submit" disabled={loading}>
                            {loading ? '로그인 중...' : '로그인'}
                        </button>
                        <div className={classes.divider}><span>또는</span></div>
                        <button type="button" className={classes.kakaoBtn} onClick={() => window.location.href = KAKAO_AUTH_URL}>
                            <img src="https://developers.kakao.com/assets/img/about/logos/kakaolink/kakaolink_btn_medium.png" alt="kakao" className={classes.kakaoIcon} />
                            카카오로 로그인
                        </button>
                    </form>
                ) : (
                    <form className={classes.form} onSubmit={handleSignup}>
                        <input className={classes.input} type="text" name="name" placeholder="이름" value={form.name} onChange={handleChange} required />
                        <input className={classes.input} type="email" name="email" placeholder="이메일" value={form.email} onChange={handleChange} required />
                        <input className={classes.input} type="password" name="password" placeholder="비밀번호" value={form.password} onChange={handleChange} required />
                        <button className={classes.submitBtn} type="submit" disabled={loading}>
                            {loading ? '가입 중...' : '회원가입'}
                        </button>
                        <div className={classes.divider}><span>또는</span></div>
                        <button type="button" className={classes.kakaoBtn} onClick={() => window.location.href = KAKAO_AUTH_URL}>
                            <img src="https://developers.kakao.com/assets/img/about/logos/kakaolink/kakaolink_btn_medium.png" alt="kakao" className={classes.kakaoIcon} />
                            카카오로 시작하기
                        </button>
                    </form>
                )}
            </div>
        </div>
    );
};

export default LoginPrompt;
