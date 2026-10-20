import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { IconEgg, IconEggCracked, IconChevronLeft, IconChevronRight, IconTrendingUp, IconTrendingDown, IconArrowsExchange } from '@tabler/icons-react';
import { IconChick, IconChicken } from '../../components/icons/GrowthIcons';
import classes from './TutorialPage.module.css';

const steps = [
    {
        id: 1,
        Icon: IconEgg,
        title: '주식이 뭐야?',
        subtitle: '알도 모르는 상태에서 시작해볼게요',
        content: [
            { type: 'analogy', text: '주식은 회사의 "조각"이에요.' },
            { type: 'explain', text: '삼성전자 주식 1주를 사면, 삼성전자의 아주 작은 주인이 되는 거예요. 회사가 잘되면 내 조각의 가치도 올라가요.' },
            { type: 'tip', text: '삐약스탁에서는 가짜 돈 1,000만원으로 연습해요. 진짜 돈은 한 푼도 안 들어요!' },
        ],
    },
    {
        id: 2,
        Icon: IconEggCracked,
        title: '어떻게 돈을 벌어?',
        subtitle: '알이 살짝 깨지기 시작했어요',
        content: [
            { type: 'analogy', text: '편의점에서 1,000원에 산 음료를 1,500원에 팔면 500원 이익!' },
            { type: 'explain', text: '주식도 똑같아요. 10,000원에 산 주식이 12,000원이 되면 2,000원 이익이에요. 반대로 떨어지면 손해를 볼 수도 있어요.' },
            { type: 'keyword', label: '매수', desc: '주식을 사는 것' },
            { type: 'keyword', label: '매도', desc: '주식을 파는 것' },
            { type: 'keyword', label: '수익률', desc: '내가 투자한 돈 대비 얼마나 벌었는지 (%)' },
        ],
    },
    {
        id: 3,
        Icon: IconChick,
        title: '시세 보는 법',
        subtitle: '병아리의 눈으로 시장을 봐요',
        content: [
            { type: 'color-guide', up: '빨간색 = 가격이 올랐어요 (좋은 신호)', down: '파란색 = 가격이 내렸어요 (주의!)' },
            { type: 'explain', text: '주가는 하루에도 수백 번 바뀌어요. 너무 자주 보면 불안해질 수 있으니, 처음엔 하루에 1~2번만 확인하는 게 좋아요.' },
            { type: 'keyword', label: '거래량', desc: '오늘 이 주식이 얼마나 많이 사고팔렸는지' },
            { type: 'keyword', label: '전일대비', desc: '어제 가격과 비교해서 얼마나 변했는지' },
            { type: 'tip', text: '삐약스탁 홈에서 급등/급락 종목을 한눈에 볼 수 있어요.' },
        ],
    },
    {
        id: 4,
        Icon: IconArrowsExchange,
        title: '첫 매수 해보기',
        subtitle: '드디어 첫 주식을 사볼 시간!',
        content: [
            { type: 'steps', items: [
                '홈 화면 검색창에서 관심있는 회사를 검색해요',
                '종목을 선택하면 현재 가격과 차트를 볼 수 있어요',
                '"주문" 탭에서 매수할 수량을 입력하고 "매수" 버튼!',
                '"내주식"에서 내가 산 주식과 수익률을 확인해요',
            ]},
            { type: 'tip', text: '처음엔 삼성전자, 카카오처럼 이름을 아는 회사부터 시작해보세요.' },
            { type: 'tip', text: '한 종목에 올인하지 말고, 2~3개 종목에 나눠서 사보는 연습을 해요.' },
        ],
    },
    {
        id: 5,
        Icon: IconChicken,
        title: '다음 단계는?',
        subtitle: '이제 투자의 세계로 날아갈 준비 완료!',
        content: [
            { type: 'next-steps', items: [
                { label: '관심종목 등록', desc: '눈여겨보는 종목을 저장해두면 가격 변동을 쉽게 추적할 수 있어요', path: '/stocks' },
                { label: '포트폴리오 확인', desc: '내 투자 현황을 한눈에 파악하고, AI가 분석 리포트도 만들어줘요', path: '/portfolio' },
                { label: 'AI 투자 도우미', desc: '모르는 게 있으면 AI 챗봇에게 물어보세요. 쉽게 설명해줘요', path: null },
                { label: 'ETF 살펴보기', desc: '주식 여러 개를 한 바구니에 담은 상품. 분산투자의 첫걸음!', path: '/etf' },
            ]},
            { type: 'tip', text: '삐약스탁에서 충분히 연습한 뒤, 실제 투자를 시작하면 실수를 줄일 수 있어요.' },
        ],
    },
];

const TutorialPage = () => {
    const navigate = useNavigate();
    const [currentStep, setCurrentStep] = useState(0);
    const step = steps[currentStep];

    const goNext = () => {
        if (currentStep < steps.length - 1) setCurrentStep(prev => prev + 1);
    };
    const goPrev = () => {
        if (currentStep > 0) setCurrentStep(prev => prev - 1);
    };

    const renderContent = (item, i) => {
        switch (item.type) {
            case 'analogy':
                return <div key={i} className={classes.analogy}>{item.text}</div>;
            case 'explain':
                return <p key={i} className={classes.explain}>{item.text}</p>;
            case 'tip':
                return <div key={i} className={classes.tip}>💡 {item.text}</div>;
            case 'keyword':
                return (
                    <div key={i} className={classes.keyword}>
                        <span className={classes.keywordLabel}>{item.label}</span>
                        <span className={classes.keywordDesc}>{item.desc}</span>
                    </div>
                );
            case 'color-guide':
                return (
                    <div key={i} className={classes.colorGuide}>
                        <div className={classes.colorUp}>
                            <IconTrendingUp size={18} stroke={1.5} />
                            <span>{item.up}</span>
                        </div>
                        <div className={classes.colorDown}>
                            <IconTrendingDown size={18} stroke={1.5} />
                            <span>{item.down}</span>
                        </div>
                    </div>
                );
            case 'steps':
                return (
                    <ol key={i} className={classes.stepsList}>
                        {item.items.map((text, j) => (
                            <li key={j} className={classes.stepsItem}>{text}</li>
                        ))}
                    </ol>
                );
            case 'next-steps':
                return (
                    <div key={i} className={classes.nextSteps}>
                        {item.items.map((ns, j) => (
                            <button
                                key={j}
                                className={classes.nextStepCard}
                                onClick={() => ns.path && navigate(ns.path)}
                                disabled={!ns.path}
                            >
                                <span className={classes.nextStepLabel}>{ns.label}</span>
                                <span className={classes.nextStepDesc}>{ns.desc}</span>
                            </button>
                        ))}
                    </div>
                );
            default:
                return null;
        }
    };

    return (
        <div className={classes.page}>
            {/* Progress bar */}
            <div className={classes.progressBar}>
                {steps.map((s, i) => (
                    <div
                        key={s.id}
                        className={`${classes.progressDot} ${i <= currentStep ? classes.progressActive : ''}`}
                    />
                ))}
            </div>
            <div className={classes.progressLabel}>{currentStep + 1} / {steps.length}</div>

            {/* Step card */}
            <div className={classes.card}>
                <div className={classes.cardIcon}>
                    <step.Icon size={48} stroke={1.5} />
                </div>
                <h1 className={classes.cardTitle}>{step.title}</h1>
                <p className={classes.cardSubtitle}>{step.subtitle}</p>
                <div className={classes.cardContent}>
                    {step.content.map(renderContent)}
                </div>
            </div>

            {/* Navigation */}
            <div className={classes.navRow}>
                <button
                    className={classes.navBtn}
                    onClick={goPrev}
                    disabled={currentStep === 0}
                >
                    <IconChevronLeft size={20} stroke={2} />
                    이전
                </button>

                {currentStep === steps.length - 1 ? (
                    <button
                        className={classes.startBtn}
                        onClick={() => navigate('/')}
                    >
                        시작하기
                    </button>
                ) : (
                    <button
                        className={classes.navBtn}
                        onClick={goNext}
                    >
                        다음
                        <IconChevronRight size={20} stroke={2} />
                    </button>
                )}
            </div>
        </div>
    );
};

export default TutorialPage;
