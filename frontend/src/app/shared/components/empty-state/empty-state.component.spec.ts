import { ComponentFixture, TestBed } from '@angular/core/testing';
import { EmptyStateComponent } from './empty-state.component';

describe('EmptyStateComponent', () => {
  let fixture: ComponentFixture<EmptyStateComponent>;
  let component: EmptyStateComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EmptyStateComponent]
    }).compileComponents();
    fixture = TestBed.createComponent(EmptyStateComponent);
    component = fixture.componentInstance;
  });

  it('renders the provided message', () => {
    component.message = 'No employees found';
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement.querySelector('.empty-state__message');
    expect(el.textContent?.trim()).toBe('No employees found');
  });

  it('renders the provided icon name', () => {
    component.icon = 'people_outline';
    fixture.detectChanges();
    const icon: HTMLElement = fixture.nativeElement.querySelector('mat-icon');
    expect(icon.textContent?.trim()).toBe('people_outline');
  });

  it('defaults to "No data found" message', () => {
    fixture.detectChanges();
    expect(component.message).toBe('No data found');
  });

  it('defaults to "inbox" icon', () => {
    fixture.detectChanges();
    expect(component.icon).toBe('inbox');
  });
});
