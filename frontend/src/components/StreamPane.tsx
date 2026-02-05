import { StreamPane as SharedStreamPane } from '@shared/components/StreamPane';
import { getToken } from '../auth/storage';
import type { ComponentProps } from 'react';

type SharedProps = ComponentProps<typeof SharedStreamPane>;
type StreamPaneProps = Omit<SharedProps, 'tokenProvider'>;

export function StreamPane(props: StreamPaneProps): JSX.Element {
  return <SharedStreamPane {...props} tokenProvider={getToken} />;
}
