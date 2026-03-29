import { Component, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProcessService } from './process.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  returnType = signal('GSTR2A');
  mode = signal('all');
  returnFile = signal<File | null>(null);
  purchaseRegisterFile = signal<File | null>(null);
  
  isProcessing = signal(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);
  downloadUrl = signal<string | null>(null);

  showPurchaseRegister = computed(() => {
    const m = this.mode();
    return m === 'reconcile' || m === 'all';
  });

  constructor(private processService: ProcessService) {}

  onFileSelected(event: any, type: 'return' | 'purchase') {
    const file = event.target.files[0];
    if (type === 'return') {
      this.returnFile.set(file);
    } else {
      this.purchaseRegisterFile.set(file);
    }
  }

  onSubmit() {
    const rf = this.returnFile();
    if (!rf) {
      this.errorMessage.set('Please select a GST Return File');
      return;
    }

    if (this.showPurchaseRegister() && !this.purchaseRegisterFile()) {
      this.errorMessage.set('Please select a Purchase Register File for Reconcile/All mode');
      return;
    }

    this.isProcessing.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);
    this.downloadUrl.set(null);

    this.processService.process(
      rf,
      this.purchaseRegisterFile(),
      this.returnType(),
      this.mode()
    ).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        this.downloadUrl.set(url);
        this.successMessage.set('Processing complete. Your download is ready.');
        this.isProcessing.set(false);
        this.triggerDownload(url, 'result.xlsx');
      },
      error: async (err) => {
        console.error(err);
        let message = 'An error occurred while processing the file.';
        if (err.error instanceof Blob) {
          const text = await err.error.text();
          try {
            const json = JSON.parse(text);
            message = json.error || message;
          } catch (e) {
            // Not JSON
          }
        }
        this.errorMessage.set(message);
        this.isProcessing.set(false);
      }
    });
  }

  private triggerDownload(url: string, filename: string) {
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
  }
}
