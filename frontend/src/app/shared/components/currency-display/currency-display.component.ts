import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-currency-display',
  standalone: true,
  template: `{{ formattedValue }}`
})
export class CurrencyDisplayComponent {
  @Input() amount = 0;
  @Input() currencyCode = 'USD';

  private static readonly SYMBOL_MAP: Record<string, string> = {
    USD: '$',
    GBP: '£',
    EUR: '€',
    INR: '₹',
    CAD: 'CA$',
    AUD: 'A$',
    SGD: 'S$'
  };

  get formattedValue(): string {
    const code = (this.currencyCode || 'USD').toUpperCase();
    const symbol = CurrencyDisplayComponent.SYMBOL_MAP[code] ?? `${code} `;
    const num = Math.round(this.amount || 0);
    return `${symbol}${num.toLocaleString('en-US')}`;
  }
}
