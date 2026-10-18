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

  it('formats EUR amounts with € symbol', () => {
    component.amount = 80000;
    component.currencyCode = 'EUR';
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent.trim()).toContain('€');
  });

  it('formats INR amounts with ₹ symbol', () => {
    component.amount = 2500000;
    component.currencyCode = 'INR';
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent.trim()).toContain('₹');
    expect(fixture.nativeElement.textContent.trim()).toContain('2,500,000');
  });

  it('formats CAD amounts with CA$ symbol', () => {
    component.amount = 95000;
    component.currencyCode = 'CAD';
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent.trim()).toContain('CA$');
  });

  it('formats AUD amounts with A$ symbol', () => {
    component.amount = 105000;
    component.currencyCode = 'AUD';
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent.trim()).toContain('A$');
  });

  it('formats SGD amounts with S$ symbol', () => {
    component.amount = 110000;
    component.currencyCode = 'SGD';
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent.trim()).toContain('S$');
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
    expect(fixture.nativeElement.textContent.trim()).not.toContain('.');
  });

  it('falls back to currency code when unknown', () => {
    component.amount = 500;
    component.currencyCode = 'XYZ';
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent.trim()).toContain('XYZ');
  });
});
