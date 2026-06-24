package kz.mybrain.superkassa.core.data.receipt

object ReceiptHtmlStyles {
    const val CSS = """
                :root {
                    --m3-background: #f8fafc;
                    --m3-on-background: #0f172a;
                    --m3-surface: #ffffff;
                    --m3-on-surface: #0f172a;
                    --m3-surface-variant: #f1f5f9;
                    --m3-on-surface-variant: #475569;
                    --m3-outline: #cbd5e1;
                    --m3-outline-variant: #e2e8f0;
                    --m3-primary: #6366f1;
                    --m3-on-primary: #ffffff;
                    --m3-primary-container: #e0e7ff;
                    --m3-on-primary-container: #312e81;
                    --m3-error: #ef4444;
                    --m3-error-container: #fee2e2;
                    --m3-on-error-container: #991b1b;
                    --m3-success: #10b981;
                    --m3-success-container: #d1fae5;
                    --m3-on-success-container: #065f46;
                    --m3-warning-container: #fef3c7;
                    --m3-on-warning-container: #92400e;
                    --m3-shadow: rgba(99, 102, 241, 0.05);
                }
                @media (prefers-color-scheme: dark) {
                    :root {
                        --m3-background: #0f172a;
                        --m3-on-background: #f8fafc;
                        --m3-surface: #1e293b;
                        --m3-on-surface: #f8fafc;
                        --m3-surface-variant: #334155;
                        --m3-on-surface-variant: #94a3b8;
                        --m3-outline: #475569;
                        --m3-outline-variant: #334155;
                        --m3-primary: #818cf8;
                        --m3-on-primary: #0f172a;
                        --m3-primary-container: #312e81;
                        --m3-on-primary-container: #e0e7ff;
                        --m3-error: #f87171;
                        --m3-error-container: #991b1b;
                        --m3-on-error-container: #fee2e2;
                        --m3-success: #34d399;
                        --m3-success-container: #065f46;
                        --m3-on-success-container: #d1fae5;
                        --m3-warning-container: #351c06;
                        --m3-on-warning-container: #fbbf24;
                        --m3-shadow: rgba(0, 0, 0, 0.3);
                    }
                }
                body.dark {
                    --m3-background: #0f172a;
                    --m3-on-background: #f8fafc;
                    --m3-surface: #1e293b;
                    --m3-on-surface: #f8fafc;
                    --m3-surface-variant: #334155;
                    --m3-on-surface-variant: #94a3b8;
                    --m3-outline: #475569;
                    --m3-outline-variant: #334155;
                    --m3-primary: #818cf8;
                    --m3-on-primary: #0f172a;
                    --m3-primary-container: #312e81;
                    --m3-on-primary-container: #e0e7ff;
                    --m3-error: #f87171;
                    --m3-error-container: #991b1b;
                    --m3-on-error-container: #fee2e2;
                    --m3-success: #34d399;
                    --m3-success-container: #065f46;
                    --m3-on-success-container: #d1fae5;
                    --m3-warning-container: #351c06;
                    --m3-on-warning-container: #fbbf24;
                    --m3-shadow: rgba(0, 0, 0, 0.3);
                }
                body.light {
                    --m3-background: #f8fafc;
                    --m3-on-background: #0f172a;
                    --m3-surface: #ffffff;
                    --m3-on-surface: #0f172a;
                    --m3-surface-variant: #f1f5f9;
                    --m3-on-surface-variant: #475569;
                    --m3-outline: #cbd5e1;
                    --m3-outline-variant: #e2e8f0;
                    --m3-primary: #6366f1;
                    --m3-on-primary: #ffffff;
                    --m3-primary-container: #e0e7ff;
                    --m3-on-primary-container: #312e81;
                    --m3-error: #ef4444;
                    --m3-error-container: #fee2e2;
                    --m3-on-error-container: #991b1b;
                    --m3-success: #10b981;
                    --m3-success-container: #d1fae5;
                    --m3-on-success-container: #065f46;
                    --m3-warning-container: #fef3c7;
                    --m3-on-warning-container: #92400e;
                    --m3-shadow: rgba(99, 102, 241, 0.05);
                }

                /* Accent Color Overrides - Light Mode */
                body.light.accent-indigo {
                    --m3-primary: #6366f1;
                    --m3-primary-container: #e0e7ff;
                    --m3-on-primary-container: #312e81;
                    --m3-shadow: rgba(99, 102, 241, 0.05);
                }
                body.light.accent-teal {
                    --m3-primary: #0d9488;
                    --m3-primary-container: #ccfbf1;
                    --m3-on-primary-container: #115e59;
                    --m3-shadow: rgba(13, 148, 136, 0.05);
                }
                body.light.accent-green {
                    --m3-primary: #16a34a;
                    --m3-primary-container: #dcfce7;
                    --m3-on-primary-container: #166534;
                    --m3-shadow: rgba(22, 163, 74, 0.05);
                }
                body.light.accent-blue {
                    --m3-primary: #2563eb;
                    --m3-primary-container: #dbeafe;
                    --m3-on-primary-container: #1e40af;
                    --m3-shadow: rgba(37, 99, 235, 0.05);
                }
                body.light.accent-orange {
                    --m3-primary: #ea580c;
                    --m3-primary-container: #ffedd5;
                    --m3-on-primary-container: #9a3412;
                    --m3-shadow: rgba(234, 88, 12, 0.05);
                }
                body.light.accent-rose {
                    --m3-primary: #e11d48;
                    --m3-primary-container: #ffe4e6;
                    --m3-on-primary-container: #9f1239;
                    --m3-shadow: rgba(225, 29, 72, 0.05);
                }

                /* Accent Color Overrides - Dark Mode */
                body.dark.accent-indigo {
                    --m3-primary: #818cf8;
                    --m3-primary-container: #312e81;
                    --m3-on-primary-container: #e0e7ff;
                    --m3-shadow: rgba(0, 0, 0, 0.3);
                }
                body.dark.accent-teal {
                    --m3-primary: #2dd4bf;
                    --m3-primary-container: #115e59;
                    --m3-on-primary-container: #ccfbf1;
                    --m3-shadow: rgba(0, 0, 0, 0.3);
                }
                body.dark.accent-green {
                    --m3-primary: #4ade80;
                    --m3-primary-container: #166534;
                    --m3-on-primary-container: #dcfce7;
                    --m3-shadow: rgba(0, 0, 0, 0.3);
                }
                body.dark.accent-blue {
                    --m3-primary: #60a5fa;
                    --m3-primary-container: #1e40af;
                    --m3-on-primary-container: #dbeafe;
                    --m3-shadow: rgba(0, 0, 0, 0.3);
                }
                body.dark.accent-orange {
                    --m3-primary: #fb923c;
                    --m3-primary-container: #9a3412;
                    --m3-on-primary-container: #ffedd5;
                    --m3-shadow: rgba(0, 0, 0, 0.3);
                }
                body.dark.accent-rose {
                    --m3-primary: #fb7185;
                    --m3-primary-container: #9f1239;
                    --m3-on-primary-container: #ffe4e6;
                    --m3-shadow: rgba(0, 0, 0, 0.3);
                }

                * { box-sizing: border-box; }
                body {
                    margin: 0;
                    padding: 20px 8px 40px 8px;
                    color: #0f172a;
                    color: var(--m3-on-background);
                    background-color: #f8fafc;
                    background-color: var(--m3-background);
                    font-family: "Inter", "Roboto", "DejaVu Sans Mono", "Courier New", monospace;
                    line-height: 1.45;
                    -webkit-font-smoothing: antialiased;
                }
                .receipt {
                    margin: 0 auto;
                    background: #ffffff;
                    background: var(--m3-surface);
                    border: 1px solid #e2e8f0;
                    border: 1px solid var(--m3-outline-variant);
                    border-top: 6px solid #6366f1;
                    border-top: 6px solid var(--m3-primary);
                    border-radius: 24px 24px 0 0;
                    box-shadow: 0 16px 48px rgba(99, 102, 241, 0.05);
                    box-shadow: 0 16px 48px var(--m3-shadow);
                    color: #0f172a;
                    color: var(--m3-on-surface);
                    position: relative;
                    padding-bottom: 28px !important;
                }
                /* Zigzag paper edge pattern at the bottom */
                .receipt::after {
                    content: "";
                    display: block;
                    position: absolute;
                    bottom: -12px;
                    left: -1px;
                    right: -1px;
                    height: 12px;
                    background-image: linear-gradient(-135deg, #ffffff 6px, transparent 0),
                                      linear-gradient(135deg, #ffffff 6px, transparent 0);
                    background-image: linear-gradient(-135deg, var(--m3-surface) 6px, transparent 0),
                                      linear-gradient(135deg, var(--m3-surface) 6px, transparent 0);
                    background-size: 12px 12px;
                    background-repeat: repeat-x;
                    filter: drop-shadow(0 4px 4px rgba(0, 0, 0, 0.05));
                }
                .receipt.tape-80mm {
                    max-width: 80mm;
                    width: 100%;
                    padding: 20px 16px;
                    font-size: 11px;
                }
                .receipt.tape-58mm {
                    max-width: 58mm;
                    width: 100%;
                    padding: 16px 8px;
                    font-size: 9.5px;
                }
                .center { text-align: center; }
                .muted { 
                    color: #475569;
                    color: var(--m3-on-surface-variant); 
                    font-size: 0.9em; 
                    margin-top: 4px; 
                }
                .rule {
                    border-top: 1px solid #e2e8f0;
                    border-top: 1px solid var(--m3-outline-variant);
                    margin: 14px 0;
                    height: 0;
                    opacity: 0.5;
                }
                .brand-header {
                    margin-bottom: 16px;
                }
                .brand-logo {
                    font-size: 1.55em;
                    font-weight: 800;
                    letter-spacing: -0.5px;
                    color: #6366f1;
                    color: var(--m3-primary);
                    margin-bottom: 6px;
                    text-transform: uppercase;
                }
                .brand-logo-img img {
                    max-height: 52px;
                    margin-bottom: 10px;
                    border-radius: 8px;
                }
                .org-title {
                    font-size: 1.05em;
                    font-weight: 700;
                    margin-bottom: 3px;
                }
                .org-bin, .org-address {
                    font-size: 0.9em;
                    color: #475569;
                    color: var(--m3-on-surface-variant);
                    margin-bottom: 3px;
                }
                .excise-stamps {
                    font-size: 0.85em;
                    color: #475569;
                    color: var(--m3-on-surface-variant);
                    margin-top: 4px;
                    background: #f1f5f9;
                    background: var(--m3-surface-variant);
                    padding: 2px 6px;
                    border-radius: 4px;
                    display: inline-block;
                    word-break: break-all;
                }
                .item-discount {
                    font-size: 0.85em;
                    color: #ef4444;
                    color: var(--m3-error);
                    margin-top: 4px;
                    font-weight: 600;
                }
                .item-markup {
                    font-size: 0.85em;
                    color: #10b981;
                    color: var(--m3-success);
                    margin-top: 4px;
                    font-weight: 600;
                }
                .doc-title {
                    font-size: 1.15em;
                    font-weight: 800;
                    color: var(--m3-primary);
                    text-transform: uppercase;
                    margin-top: 8px;
                    letter-spacing: 0.8px;
                }
                .doc-title-divider {
                    width: 50px;
                    border-top: 2px solid var(--m3-outline);
                    margin: 8px auto 4px auto;
                    opacity: 0.6;
                }
                .meta-table, .items-table, .payments-table, .summary-table, .tax-table {
                    width: 100%;
                    border-collapse: collapse;
                }
                .meta-table td {
                    padding: 4px 0;
                    vertical-align: middle;
                }
                .meta-table td:first-child {
                    width: 55%;
                    color: #475569;
                    color: var(--m3-on-surface-variant);
                    font-weight: 500;
                }
                .meta-table td:last-child {
                    text-align: right;
                    font-weight: 600;
                    color: #0f172a;
                    color: var(--m3-on-surface);
                }
                .items-table, .tax-table {
                    table-layout: fixed;
                }
                .items-table th, .tax-table th {
                    font-size: 0.8em;
                    color: #475569;
                    color: var(--m3-on-surface-variant);
                    text-transform: uppercase;
                    letter-spacing: 0.8px;
                    border-bottom: 2px solid #e2e8f0;
                    border-bottom: 2px solid var(--m3-outline-variant);
                    padding: 8px 4px;
                    text-align: left;
                    font-weight: 700;
                    word-wrap: break-word;
                }
                .items-table th.num, .tax-table th.num {
                    text-align: right;
                }
                .items-table td, .tax-table td {
                    padding: 8px 4px;
                    border-bottom: 1px solid #e2e8f0;
                    border-bottom: 1px solid var(--m3-outline-variant);
                    vertical-align: top;
                }
                .items-table th:first-child, .items-table td:first-child {
                    padding-left: 0;
                    width: 34%;
                }
                .items-table th:nth-child(2), .items-table td:nth-child(2) {
                    width: 22%;
                }
                .items-table th:nth-child(3), .items-table td:nth-child(3) {
                    width: 22%;
                }
                .items-table th:last-child, .items-table td:last-child {
                    padding-right: 0;
                    width: 22%;
                }
                .tax-table th:first-child, .tax-table td:first-child {
                    padding-left: 0;
                    width: 40%;
                }
                .tax-table th:nth-child(2), .tax-table td:nth-child(2) {
                    width: 30%;
                }
                .tax-table th:last-child, .tax-table td:last-child {
                    padding-right: 0;
                    width: 30%;
                }
                .tape-58mm .items-table th:first-child, .tape-58mm .items-table td:first-child {
                    width: 30%;
                }
                .tape-58mm .items-table th:nth-child(2), .tape-58mm .items-table td:nth-child(2) {
                    width: 22%;
                }
                .tape-58mm .items-table th:nth-child(3), .tape-58mm .items-table td:nth-child(3) {
                    width: 24%;
                }
                .tape-58mm .items-table th:last-child, .tape-58mm .items-table td:last-child {
                    width: 24%;
                }
                .payments-table td, .summary-table td {
                    padding: 5px 0;
                    vertical-align: middle;
                }
                .num {
                    text-align: right;
                }
                td.num {
                    white-space: nowrap;
                    font-weight: 500;
                }
                .name {
                    padding-right: 8px;
                    word-break: break-word;
                    font-weight: 600;
                }
                .summary-table .grand td {
                    font-weight: 800;
                    font-size: 1.25em;
                    color: #6366f1;
                    color: var(--m3-primary);
                    border-top: 2px solid #6366f1;
                    border-top: 2px solid var(--m3-primary);
                    padding-top: 10px;
                }
                .section-title {
                    font-size: 0.9em;
                    font-weight: 800;
                    text-transform: uppercase;
                    color: #475569;
                    color: var(--m3-on-surface-variant);
                    margin-top: 16px;
                    margin-bottom: 8px;
                    letter-spacing: 0.8px;
                }
                .footer {
                    margin-top: 16px;
                    font-size: 0.9em;
                    color: #475569;
                    color: var(--m3-on-surface-variant);
                    line-height: 1.5;
                }
                .footer-item {
                    margin-bottom: 4px;
                }
                .receipt-link {
                    margin-top: 16px;
                    word-break: break-all;
                    background-color: #f1f5f9;
                    background-color: var(--m3-surface-variant);
                    border: 1px solid #e2e8f0;
                    border: 1px solid var(--m3-outline-variant);
                    border-radius: 12px;
                    padding: 10px 14px;
                    font-size: 0.85em;
                    color: #475569;
                    color: var(--m3-on-surface-variant);
                    text-align: center;
                }
                .receipt-link a {
                    color: #6366f1;
                    color: var(--m3-primary);
                    text-decoration: none;
                    font-weight: 600;
                    display: block;
                    margin-top: 4px;
                }
                .receipt-link a:hover {
                    text-decoration: underline;
                }
                .qr {
                    margin-top: 16px;
                    text-align: center;
                }
                .qr img {
                    width: 130px;
                    height: 130px;
                    border: 1px solid #e2e8f0;
                    border: 1px solid var(--m3-outline-variant);
                    border-radius: 16px;
                    padding: 8px;
                    background: #ffffff;
                    box-shadow: 0 4px 12px rgba(99, 102, 241, 0.05);
                    box-shadow: 0 4px 12px var(--m3-shadow);
                }
                .qr::after {
                    content: "Scan to verify / Тексеру үшін сканерлеңіз";
                    display: block;
                    margin-top: 6px;
                    font-size: 0.8em;
                    color: #475569;
                    color: var(--m3-on-surface-variant);
                    text-transform: uppercase;
                    letter-spacing: 0.5px;
                }
                .badge {
                    display: inline-flex;
                    flex-direction: column;
                    align-items: center;
                    justify-content: center;
                    padding: 6px 14px;
                    font-size: 0.8em;
                    font-weight: 700;
                    border-radius: 10px;
                    text-transform: uppercase;
                    letter-spacing: 0.5px;
                    line-height: 1.15;
                    text-align: center;
                    box-shadow: 0 2px 4px rgba(99, 102, 241, 0.05);
                    box-shadow: 0 2px 4px var(--m3-shadow);
                }
                .badge-main {
                    display: block;
                }
                .badge-divider {
                    display: block;
                    width: 100%;
                    height: 1px;
                    margin: 3px 0;
                    background-color: currentColor;
                    opacity: 0.25;
                }
                .badge-sub {
                    display: block;
                    font-size: 0.85em;
                    opacity: 0.8;
                }
                .badge-success {
                    background-color: #d1fae5;
                    background-color: var(--m3-success-container);
                    color: #065f46;
                    color: var(--m3-on-success-container);
                }
                .badge-warning {
                    background-color: #fef3c7;
                    background-color: var(--m3-warning-container);
                    color: #92400e;
                    color: var(--m3-on-warning-container);
                }
                .badge-error {
                    background-color: #fee2e2;
                    background-color: var(--m3-error-container);
                    color: #991b1b;
                    color: var(--m3-on-error-container);
                }
                .badge-neutral {
                    background-color: #e2e8f0;
                    background-color: var(--m3-surface-variant);
                    color: #475569;
                    color: var(--m3-on-surface-variant);
                }
                .lang-kk {
                    display: block;
                    font-weight: inherit;
                }
                .lang-sep {
                    display: none;
                }
                .lang-ru {
                    display: block;
                    font-size: 0.82em;
                    opacity: 0.6;
                    margin-top: 2px;
                    font-weight: normal !important;
                }
                .bold { font-weight: 700; }
                
                /* Modern card list layouts */
                .items-list, .taxes-list {
                    width: 100%;
                }
                .items-list > :last-child, .taxes-list > :last-child {
                    margin-bottom: 0 !important;
                }
                
                /* Custom branding containers */
                 .custom-before-header-container,
                 .custom-header-container,
                 .custom-after-header-container,
                 .custom-before-items-container,
                 .custom-after-items-container,
                 .custom-before-totals-container,
                 .custom-after-totals-container,
                 .custom-before-qr-container,
                 .custom-footer-container {
                     line-height: 1.45;
                     color: #475569;
                     color: var(--m3-on-surface-variant);
                     font-size: 0.9em;
                 }
                 .custom-before-header-container {
                     margin-bottom: 12px;
                 }
                 .custom-header-container {
                     margin-bottom: 12px;
                 }
                 .custom-after-header-container {
                     margin-top: 12px;
                     margin-bottom: 4px;
                 }
                 .custom-before-items-container {
                     margin-bottom: 12px;
                     text-align: left;
                 }
                 .custom-after-items-container {
                     margin-top: 12px;
                     margin-bottom: 12px;
                     text-align: left;
                 }
                 .custom-before-totals-container {
                     margin-top: 8px;
                     margin-bottom: 8px;
                     text-align: left;
                 }
                 .custom-after-totals-container {
                     margin-top: 12px;
                     margin-bottom: 12px;
                     text-align: left;
                 }
                 .custom-before-qr-container {
                     margin-top: 12px;
                     margin-bottom: 8px;
                     text-align: center;
                 }
                 .custom-footer-container {
                     margin-top: 12px;
                 }
                .item-row-card {
                    background: #f1f5f9;
                    background: var(--m3-surface-variant);
                    border-radius: 12px;
                    padding: 8px 12px;
                    margin-bottom: 8px;
                    border: 1px solid #e2e8f0;
                    border: 1px solid var(--m3-outline-variant);
                }
                .item-row-table {
                    width: 100%;
                    border-collapse: collapse;
                }
                .item-row-table td {
                    padding: 2px 0;
                    border: none;
                }
                .item-name-cell {
                    font-weight: 700;
                    color: #0f172a;
                    color: var(--m3-on-surface);
                    word-break: break-word;
                    font-size: 1.05em;
                }
                .item-sum-cell {
                    text-align: right;
                    white-space: nowrap;
                    font-weight: 700;
                    color: #0f172a;
                    color: var(--m3-on-surface);
                    vertical-align: top;
                }
                .item-details-cell {
                    font-size: 0.9em;
                    color: #475569;
                    color: var(--m3-on-surface-variant);
                    line-height: 1.4;
                }
                
                .tax-row-card {
                    background: #ffffff;
                    background: var(--m3-surface);
                    border: 1px solid #e2e8f0;
                    border: 1px solid var(--m3-outline-variant);
                    border-radius: 12px;
                    padding: 8px 12px;
                    margin-bottom: 6px;
                }
                .tax-row-card.highlighted {
                    border: 2px solid #6366f1;
                    border: 2px solid var(--m3-primary);
                    background: #f8fafc;
                    background: var(--m3-surface-variant);
                }
                .tax-row-card.dashed {
                    border-style: dashed;
                }
                .tax-row-table {
                    width: 100%;
                    border-collapse: collapse;
                }
                .tax-row-table td {
                    padding: 2px 0;
                    border: none;
                }
                .tax-rate-cell {
                    color: #0f172a;
                    color: var(--m3-on-surface);
                }
                .tax-sum-cell {
                    text-align: right;
                    white-space: nowrap;
                    color: #6366f1;
                    color: var(--m3-primary);
                }
                .tax-details-cell {
                    font-size: 0.9em;
                    color: #475569;
                    color: var(--m3-on-surface-variant);
                }


                
                /* Word split prevention on tables */
                .meta-table td, .summary-table td, .parent-ticket-table td, .tax-row-table td, .item-row-table td {
                    word-break: keep-all;
                    word-wrap: normal;
                }

                /* Storno item styling */
                .storno-item .item-name-cell {
                    text-decoration: line-through;
                    opacity: 0.6;
                }
                .storno-item .item-sum-cell {
                    text-decoration: line-through;
                    opacity: 0.6;
                }
                .storno-item .item-details-cell {
                    opacity: 0.7;
                }
                .storno-badge {
                    display: inline-block;
                    background-color: #fee2e2;
                    background-color: var(--m3-error-container);
                    color: #991b1b;
                    color: var(--m3-on-error-container);
                    font-size: 0.75em;
                    font-weight: 700;
                    padding: 1px 5px;
                    border-radius: 4px;
                    margin-left: 6px;
                    text-transform: uppercase;
                    vertical-align: middle;
                }

                /* Item VAT Badge */
                .item-vat {
                    font-size: 0.85em;
                    color: #475569;
                    color: var(--m3-on-surface-variant);
                    margin-top: 4px;
                    font-weight: 600;
                }

                /* Parent Ticket Info Card (Storno Warning Block) */
                .parent-ticket-card {
                    background-color: #fef3c7;
                    background-color: var(--m3-warning-container);
                    color: #92400e;
                    color: var(--m3-on-warning-container);
                    border: 1px solid #cbd5e1;
                    border: 1px solid var(--m3-outline-variant);
                    border-radius: 12px;
                    padding: 8px 12px;
                    margin-bottom: 8px;
                }
                .parent-ticket-table {
                    width: 100%;
                    border-collapse: collapse;
                }
                .parent-ticket-table td {
                    padding: 2px 0;
                    border: none;
                }
                .parent-ticket-table td:first-child {
                    width: 55%;
                    font-size: 0.9em;
                }
                .parent-ticket-table td:last-child {
                    text-align: right;
                    font-weight: 700;
                }
                
                @media print {
                    :root, body, body.dark {
                        --m3-background: #ffffff !important;
                        --m3-on-background: #000000 !important;
                        --m3-surface: #ffffff !important;
                        --m3-on-surface: #000000 !important;
                        --m3-surface-variant: #f0f0f0 !important;
                        --m3-on-surface-variant: #333333 !important;
                        --m3-outline: #000000 !important;
                        --m3-outline-variant: #000000 !important;
                        --m3-primary: #000000 !important;
                        --m3-on-primary: #ffffff !important;
                        --m3-error: #000000 !important;
                        --m3-success: #000000 !important;
                        --m3-shadow: none !important;
                    }
                    body { padding: 0; }
                    .receipt { border: 0; max-width: none; padding: 0; box-shadow: none; border-radius: 0 !important; }
                    .receipt::after { display: none !important; }
                    .item-row-card, .tax-row-card {
                        background: transparent !important;
                        border: none !important;
                        border-radius: 0 !important;
                        padding: 6px 0 !important;
                        margin-bottom: 0 !important;
                        border-bottom: 1px solid #000000 !important;
                    }
                    .parent-ticket-card {
                        background: transparent !important;
                        border: 1px solid #000000 !important;
                        color: #000000 !important;
                        border-radius: 0 !important;
                        padding: 6px 0 !important;
                        margin-bottom: 0 !important;
                        border-bottom: 1px solid #000000 !important;
                    }
                    .lang-kk {
                        color: #000000 !important;
                        opacity: 1 !important;
                    }
                    .badge { color: #000000 !important; border: 1px solid #000000; padding: 1px 4px; border-radius: 0; box-shadow: none; }
                    .receipt-link { border: 1px solid #000000; background-color: transparent; border-radius: 0; }
                    .receipt-link a { color: #000000; }
                    .qr img { border-radius: 0; border: 1px solid #000000; box-shadow: none; }
                    .qr::after { display: none !important; }
                }
    """
}
