import React from 'react';

/**
 * Custom growth stage icons matching Tabler icon style.
 * 24x24 viewBox, stroke-based, strokeLinecap/strokeLinejoin round.
 */

export const IconChick = ({ size = 24, stroke = 1.5, className, ...props }) => (
    <svg
        xmlns="http://www.w3.org/2000/svg"
        width={size}
        height={size}
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth={stroke}
        strokeLinecap="round"
        strokeLinejoin="round"
        className={className}
        {...props}
    >
        {/* head */}
        <circle cx="12" cy="7.5" r="3.5" />
        {/* body */}
        <ellipse cx="12" cy="15.5" rx="5.5" ry="4.5" />
        {/* beak */}
        <path d="M15.5 7l2 .5-2 1" />
        {/* eye */}
        <circle cx="13.5" cy="7" r=".6" fill="currentColor" stroke="none" />
        {/* wing */}
        <path d="M7.5 13.5c-1.5 1-1.5 3.5 0 4.5" />
        {/* feet */}
        <path d="M10 20v2" />
        <path d="M14 20v2" />
    </svg>
);

export const IconChicken = ({ size = 24, stroke = 1.5, className, ...props }) => (
    <svg
        xmlns="http://www.w3.org/2000/svg"
        width={size}
        height={size}
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth={stroke}
        strokeLinecap="round"
        strokeLinejoin="round"
        className={className}
        {...props}
    >
        {/* comb */}
        <path d="M12 4c.5-1.5 1.5-1.5 2 0 .5-1.5 1.5-1.5 2 0" />
        {/* head */}
        <circle cx="14" cy="7" r="3" />
        {/* body */}
        <ellipse cx="11" cy="15" rx="6" ry="5" />
        {/* beak */}
        <path d="M17 6.5l2 .5-2 1" />
        {/* eye */}
        <circle cx="15.5" cy="6.5" r=".6" fill="currentColor" stroke="none" />
        {/* wattle */}
        <path d="M16 9c.5 1 0 2-.5 2" />
        {/* wing */}
        <path d="M6.5 13c-2 1.5-2 4 0 5" />
        {/* tail */}
        <path d="M5 13.5c-1-1.5-.5-3.5 1-4.5" />
        {/* feet */}
        <path d="M9 20l-1 2.5M9 22.5h2" />
        <path d="M13 20l1 2.5M14 22.5h-2" />
    </svg>
);
