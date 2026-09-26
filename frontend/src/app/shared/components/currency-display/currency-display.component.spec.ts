import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CurrencyDisplayComponent } from './currency-display.component';

describe('CurrencyDisplayComponent', () => {
  let fixture: ComponentFixture<CurrencyDisplayComponent>;
  let component: CurrencyDisplayComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CurrencyDisplayComponent]
    }).compileComponents();
    fixture = TestBed.createComponent(CurrencyDisplayComponent);
    component = fixture.componentInstance;
  });

  it('formats USD amounts with $ symbol', () => {
    component.amount = 90000;
    component.currencyCode = 'USD';
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent.trim()).toContain('$');
    expect(fixture.nativeElement.textContent.trim()).toContain('90,000');
  });

  it('formats GBP amounts with £ symbol', () => {
    component.amount = 75000;
    component.currencyCode = 'GBP';
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent.trim()).toContain('£');
  });

  it('defaults to 0 amount and USD currency', () => {
    fixture.detectChanges();
    expect(component.amount).toBe(0);
    expect(component.currencyCode).toBe('USD');
  });

  it('renders zero amount without decimal places', () => {
    component.amount = 0;
    component.currencyCode = 'USD';
    fixture.detectChanges();
    // format is '1.0-0' so no decimals
    expect(fixture.nativeElement.textContent.trim()).not.toContain('.');
  });
});
