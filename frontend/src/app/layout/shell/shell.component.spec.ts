import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { ShellComponent } from './shell.component';

describe('ShellComponent', () => {
  let fixture: ComponentFixture<ShellComponent>;
  let component: ShellComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ShellComponent],
      providers: [
        provideAnimationsAsync(),
        provideRouter([])
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ShellComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('renders the brand title in the header toolbar', () => {
    const toolbar: HTMLElement = fixture.nativeElement.querySelector('.shell__toolbar');
    expect(toolbar.textContent).toContain('ACME Compensation Hub');
  });

  it('contains navigation items for Employees and Analytics', () => {
    expect(component.navItems.length).toBe(2);
    expect(component.navItems[0].label).toBe('Employees');
    expect(component.navItems[0].route).toBe('/employees');
    expect(component.navItems[1].label).toBe('Analytics');
    expect(component.navItems[1].route).toBe('/analytics');
  });

  it('renders navigation links in the sidenav list', () => {
    const navLinks: NodeListOf<HTMLAnchorElement> = fixture.nativeElement.querySelectorAll('mat-nav-list a');
    expect(navLinks.length).toBe(2);
    const linkTexts = Array.from(navLinks).map(a => a.textContent?.trim());
    expect(linkTexts.some(t => t?.includes('Employees'))).toBeTrue();
    expect(linkTexts.some(t => t?.includes('Analytics'))).toBeTrue();
  });

  it('renders router-outlet for feature views', () => {
    const outlet = fixture.nativeElement.querySelector('router-outlet');
    expect(outlet).toBeTruthy();
  });
});
