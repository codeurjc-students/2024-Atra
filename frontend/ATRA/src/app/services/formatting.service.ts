import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class FormattingService {

  static formatPace(value: number): string {
      const minutes = Math.floor(value / 60);
      const seconds = value % 60;
      console.log(value);

      return FormattingService.formatTime(value)//`${minutes}:${seconds.toFixed(0).padStart(2, '0')}`; // Format as mm:ss 1500 100 500
    }

  static formatTime(seconds: number): string { //secsToHHMMSS
    //apparently new Date(seconds * 1000).toISOString().substring(11, 19) does the same thing, so long as total time is less than 24 hours

    // Total number of seconds in the difference
    const totalSeconds = Math.floor(seconds);
    // Total number of minutes in the difference
    const totalMinutes = Math.floor(totalSeconds / 60);
    // Total number of hours in the difference
    const totalHours = Math.floor(totalMinutes / 60);
    // Getting the number of seconds left in one minute
    const remSeconds = totalSeconds % 60;

    // Getting the number of minutes left in one hour
    const remMinutes = totalMinutes % 60;

    const hoursString = totalHours != 0 ? totalHours.toString()+":":""
    const minsString = (remMinutes < 10 && totalHours!=0) ? "0"+remMinutes.toString():remMinutes.toString()
    const secsString = remSeconds < 10 ? "0"+remSeconds.toString():remSeconds.toString()



    return `${hoursString}${minsString}:${secsString}`
  }

  static formatDateTime(dateTime: Date): string {
    const hours = dateTime.getHours();
    const minutes = dateTime.getMinutes()<10 ? '0'+dateTime.getMinutes():dateTime.getMinutes()
    return `${hours}:${minutes}`
  }

}
