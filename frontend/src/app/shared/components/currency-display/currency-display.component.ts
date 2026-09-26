import { Component, Input } from '@angular/core';
import { CurrencyPipe } from '@angular/common';

@Component({
  selector: 'app-currency-display',
  standalone: true,
  imports: [CurrencyPipe],
  template: `{{ amount | currency: currencyCode : 'symbol' : '1.0-0' }}`
})
export class CurrencyDisplayComponent {
  @Input() amount = 0;
  @Input() currencyCode = 'USD';
}
