from __future__ import annotations

from concurrent.futures import Future, ThreadPoolExecutor, wait
from threading import Lock
from typing import Callable, TypeVar

T = TypeVar("T")


class InProcessJobRunner:
    """Lightweight in-process runner for async job execution in the scaffold phase."""

    def __init__(self, max_workers: int = 2) -> None:
        self._executor = ThreadPoolExecutor(max_workers=max_workers, thread_name_prefix="aipa-job")
        self._lock = Lock()
        self._futures: set[Future[object]] = set()

    def submit(self, fn: Callable[[], T]) -> Future[T]:
        future = self._executor.submit(fn)
        with self._lock:
            self._futures.add(future)
        future.add_done_callback(self._on_done)
        return future

    def wait_for_idle(self, timeout: float | None = None) -> None:
        with self._lock:
            snapshot = list(self._futures)
        if snapshot:
            wait(snapshot, timeout=timeout)

    def shutdown(self, wait_for_tasks: bool = True) -> None:
        self._executor.shutdown(wait=wait_for_tasks, cancel_futures=False)

    def _on_done(self, future: Future[object]) -> None:
        with self._lock:
            self._futures.discard(future)

