import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ProcessService {
  private apiUrl = 'http://localhost:8080/api/process';

  constructor(private http: HttpClient) {}

  process(
    returnFile: File,
    purchaseRegisterFile: File | null,
    returnType: string,
    mode: string
  ): Observable<Blob> {
    const formData = new FormData();
    formData.append('returnFile', returnFile);
    if (purchaseRegisterFile) {
      formData.append('purchaseRegisterFile', purchaseRegisterFile);
    }
    formData.append('returnType', returnType);
    formData.append('mode', mode);

    return this.http.post(this.apiUrl, formData, {
      responseType: 'blob'
    });
  }
}
