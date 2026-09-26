import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { ConfirmDialogComponent } from './confirm-dialog.component';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';

describe('ConfirmDialogComponent', () => {
  let fixture: ComponentFixture<ConfirmDialogComponent>;
  let component: ConfirmDialogComponent;
  let dialogRefSpy: jasmine.SpyObj<MatDialogRef<ConfirmDialogComponent>>;

  beforeEach(async () => {
    dialogRefSpy = jasmine.createSpyObj('MatDialogRef', ['close']);

    await TestBed.configureTestingModule({
      imports: [ConfirmDialogComponent],
      providers: [
        provideAnimationsAsync(),
        { provide: MatDialogRef, useValue: dialogRefSpy },
        {
          provide: MAT_DIALOG_DATA,
          useValue: { title: 'Confirm Action', message: 'Are you sure?', confirmLabel: 'Yes', cancelLabel: 'No' }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ConfirmDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('creates the component', () => {
    expect(component).toBeTruthy();
  });

  it('renders the title from injected data', () => {
    const title: HTMLElement = fixture.nativeElement.querySelector('[mat-dialog-title]');
    expect(title.textContent?.trim()).toBe('Confirm Action');
  });

  it('renders the message from injected data', () => {
    const content: HTMLElement = fixture.nativeElement.querySelector('mat-dialog-content');
    expect(content.textContent?.trim()).toBe('Are you sure?');
  });

  it('closes with false when cancel is clicked', () => {
    const buttons: NodeListOf<HTMLButtonElement> = fixture.nativeElement.querySelectorAll('button');
    const cancelBtn = Array.from(buttons).find(b => b.textContent?.trim() === 'No');
    cancelBtn?.click();
    expect(dialogRefSpy.close).toHaveBeenCalledWith(false);
  });

  it('closes with true when confirm is clicked', () => {
    const buttons: NodeListOf<HTMLButtonElement> = fixture.nativeElement.querySelectorAll('button');
    const confirmBtn = Array.from(buttons).find(b => b.textContent?.trim() === 'Yes');
    confirmBtn?.click();
    expect(dialogRefSpy.close).toHaveBeenCalledWith(true);
  });

  it('uses default labels when confirmLabel and cancelLabel are not provided', async () => {
    await TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [ConfirmDialogComponent],
      providers: [
        provideAnimationsAsync(),
        { provide: MatDialogRef, useValue: dialogRefSpy },
        { provide: MAT_DIALOG_DATA, useValue: { title: 'Test', message: 'Test?' } }
      ]
    }).compileComponents();
    const f = TestBed.createComponent(ConfirmDialogComponent);
    f.detectChanges();
    const buttons: NodeListOf<HTMLButtonElement> = f.nativeElement.querySelectorAll('button');
    const texts = Array.from(buttons).map(b => b.textContent?.trim());
    expect(texts).toContain('Cancel');
    expect(texts).toContain('Confirm');
  });
});
