import { ComponentFixture, TestBed } from '@angular/core/testing';
import { StatusBadgeComponent } from './status-badge.component';

describe('StatusBadgeComponent', () => {
  let fixture: ComponentFixture<StatusBadgeComponent>;
  let component: StatusBadgeComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StatusBadgeComponent]
    }).compileComponents();
    fixture = TestBed.createComponent(StatusBadgeComponent);
    component = fixture.componentInstance;
  });

  it('renders "Active" text for ACTIVE status', () => {
    component.status = 'ACTIVE';
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement.querySelector('.badge');
    expect(el.textContent?.trim()).toBe('Active');
  });

  it('renders "Inactive" text for INACTIVE status', () => {
    component.status = 'INACTIVE';
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement.querySelector('.badge');
    expect(el.textContent?.trim()).toBe('Inactive');
  });

  it('applies badge--active class for ACTIVE status', () => {
    component.status = 'ACTIVE';
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement.querySelector('.badge');
    expect(el.classList).toContain('badge--active');
    expect(el.classList).not.toContain('badge--inactive');
  });

  it('applies badge--inactive class for INACTIVE status', () => {
    component.status = 'INACTIVE';
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement.querySelector('.badge');
    expect(el.classList).toContain('badge--inactive');
    expect(el.classList).not.toContain('badge--active');
  });

  it('defaults to ACTIVE when no input is provided', () => {
    fixture.detectChanges();
    expect(component.status).toBe('ACTIVE');
  });
});
