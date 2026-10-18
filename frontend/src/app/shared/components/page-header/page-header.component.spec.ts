import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PageHeaderComponent } from './page-header.component';

describe('PageHeaderComponent', () => {
  let fixture: ComponentFixture<PageHeaderComponent>;
  let component: PageHeaderComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PageHeaderComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(PageHeaderComponent);
    component = fixture.componentInstance;
  });

  it('renders the title in an h1 element', () => {
    component.title = 'Employee Directory';
    fixture.detectChanges();
    const titleEl: HTMLElement = fixture.nativeElement.querySelector('.page-header__title');
    expect(titleEl.textContent?.trim()).toBe('Employee Directory');
  });

  it('renders the subtitle when provided', () => {
    component.title = 'Employees';
    component.subtitle = 'View and manage all active employees';
    fixture.detectChanges();
    const subtitleEl: HTMLElement = fixture.nativeElement.querySelector('.page-header__subtitle');
    expect(subtitleEl).toBeTruthy();
    expect(subtitleEl.textContent?.trim()).toBe('View and manage all active employees');
  });

  it('omits subtitle element when subtitle is empty', () => {
    component.title = 'Employees';
    component.subtitle = '';
    fixture.detectChanges();
    const subtitleEl = fixture.nativeElement.querySelector('.page-header__subtitle');
    expect(subtitleEl).toBeNull();
  });

  it('renders the provided icon', () => {
    component.icon = 'people';
    fixture.detectChanges();
    const iconEl: HTMLElement = fixture.nativeElement.querySelector('mat-icon');
    expect(iconEl.textContent?.trim()).toBe('people');
  });

  it('defaults to "info" icon when none provided', () => {
    fixture.detectChanges();
    expect(component.icon).toBe('info');
    const iconEl: HTMLElement = fixture.nativeElement.querySelector('mat-icon');
    expect(iconEl.textContent?.trim()).toBe('info');
  });
});
